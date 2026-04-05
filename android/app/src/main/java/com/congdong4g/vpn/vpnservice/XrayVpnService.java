package com.congdong4g.vpn.vpnservice;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.congdong4g.vpn.modules.XrayVpnModule;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
public class XrayVpnService extends VpnService {
    private static final String TAG="XrayVpnService";
    public static final String CHANNEL_ID="vpn_channel",ACTION_START="START",ACTION_STOP="STOP",ACTION_QUERY_STATS="QUERY_STATS",EXTRA_CONFIG="config",EXTRA_SERVER_NAME="server_name";
    public static final int NOTIF_ID=1001;
    private ParcelFileDescriptor tunInterface;
    private Thread statsThread;
    private volatile boolean running=false;
    private final AtomicLong bytesIn=new AtomicLong(0),bytesOut=new AtomicLong(0);
    private long startTime;
    @Override public void onCreate(){super.onCreate();createNotificationChannel();}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        switch(intent.getAction()==null?"":intent.getAction()){
            case ACTION_START:startVpn(intent.getStringExtra(EXTRA_CONFIG),intent.getStringExtra(EXTRA_SERVER_NAME));break;
            case ACTION_STOP:stopVpn();break;
            case ACTION_QUERY_STATS:emitStats();break;
        }
        return START_STICKY;
    }
    @Override public void onDestroy(){stopVpn();super.onDestroy();}
    private void startVpn(String cfg,String srv){
        XrayVpnModule.sendStatusEvent("connecting",null);
        startForeground(NOTIF_ID,buildNotification("Đang kết nối...",srv));
        try{
            File cfgFile=writeConfig(cfg);
            tunInterface=new Builder().setSession("CongDong4G VPN").addAddress("10.10.0.2",24).addRoute("0.0.0.0",0).addDnsServer("1.1.1.1").addDnsServer("8.8.8.8").setMtu(1500).establish();
            if(tunInterface==null)throw new IOException("VPN interface null");
            int tunFd=tunInterface.getFd();protect(tunFd);
            startXrayCore(cfgFile.getAbsolutePath(),tunFd);
            running=true;startTime=System.currentTimeMillis();startStatsThread();
            updateNotification("🔒 Đã kết nối",srv);
            XrayVpnModule.sendStatusEvent("connected",null);
        }catch(Exception e){XrayVpnModule.sendStatusEvent("error",e.getMessage());stopForeground(true);stopSelf();}
    }
    private void stopVpn(){
        XrayVpnModule.sendStatusEvent("disconnecting",null);running=false;stopXrayCore();
        if(tunInterface!=null){try{tunInterface.close();}catch(IOException e){}tunInterface=null;}
        if(statsThread!=null){statsThread.interrupt();statsThread=null;}
        stopForeground(true);stopSelf();XrayVpnModule.sendStatusEvent("disconnected",null);
    }
    private void startXrayCore(String path,int fd){
        try{Class<?> lib=Class.forName("libXray.LibXray");lib.getMethod("startXray",String.class).invoke(null,path);}
        catch(ClassNotFoundException e){Log.w(TAG,"libXray not found - stub mode");}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private void stopXrayCore(){try{Class<?> lib=Class.forName("libXray.LibXray");lib.getMethod("stopXray").invoke(null);}catch(Exception e){}}
    private File writeConfig(String json)throws IOException{File d=new File(getFilesDir(),"xray");if(!d.exists())d.mkdirs();File f=new File(d,"config.json");try(FileWriter fw=new FileWriter(f)){fw.write(json);}return f;}
    private void startStatsThread(){
        statsThread=new Thread(()->{long pI=0,pO=0;while(running&&!Thread.currentThread().isInterrupted()){try{Thread.sleep(1000);long cI=bytesIn.get(),cO=bytesOut.get(),el=(System.currentTimeMillis()-startTime)/1000;XrayVpnModule.sendStatsEvent(cI,cO,cI-pI,cO-pO,el);pI=cI;pO=cO;}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}},"stats-thread");
        statsThread.setDaemon(true);statsThread.start();
    }
    private void emitStats(){long el=running?(System.currentTimeMillis()-startTime)/1000:0;XrayVpnModule.sendStatsEvent(bytesIn.get(),bytesOut.get(),0,0,el);}
    private void createNotificationChannel(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"VPN Status",NotificationManager.IMPORTANCE_LOW);ch.setDescription("CongDong4G VPN");getSystemService(NotificationManager.class).createNotificationChannel(ch);}}
    private Notification buildNotification(String title,String text){return new NotificationCompat.Builder(this,CHANNEL_ID).setContentTitle(title).setContentText(text).setSmallIcon(android.R.drawable.ic_lock_lock).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build();}
    private void updateNotification(String title,String text){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF_ID,buildNotification(title,text));}
}
