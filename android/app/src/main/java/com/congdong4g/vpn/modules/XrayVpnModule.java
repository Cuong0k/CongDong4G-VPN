package com.congdong4g.vpn.modules;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.congdong4g.vpn.vpnservice.XrayVpnService;
import java.util.HashMap;
import java.util.Map;
public class XrayVpnModule extends ReactContextBaseJavaModule {
    private static final String TAG="XrayVpnModule";
    private static XrayVpnModule instance;
    private final ReactApplicationContext reactContext;
    private static final int VPN_PERMISSION_REQ=100;
    private Promise pendingPermissionPromise;
    private String pendingConfig,pendingServerName;
    public XrayVpnModule(ReactApplicationContext ctx){super(ctx);this.reactContext=ctx;instance=this;}
    @NonNull @Override public String getName(){return "XrayVpn";}
    public static void sendEvent(String name,Map<String,Object> p){if(instance==null)return;instance.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(name,p);}
    @ReactMethod public void startVpn(String cfg,String srv,Promise p){Intent pi=VpnService.prepare(reactContext);if(pi!=null){pendingPermissionPromise=p;pendingConfig=cfg;pendingServerName=srv;getCurrentActivity().startActivityForResult(pi,VPN_PERMISSION_REQ);return;}doStartVpn(cfg,srv,p);}
    private void doStartVpn(String cfg,String srv,Promise p){Intent i=new Intent(reactContext,XrayVpnService.class);i.putExtra(XrayVpnService.EXTRA_CONFIG,cfg);i.putExtra(XrayVpnService.EXTRA_SERVER_NAME,srv);i.setAction(XrayVpnService.ACTION_START);reactContext.startService(i);p.resolve(null);}
    public void onVpnPermissionResult(boolean granted){if(pendingPermissionPromise==null)return;if(granted){doStartVpn(pendingConfig,pendingServerName,pendingPermissionPromise);}else{sendStatusEvent("error","Quyền VPN bị từ chối");pendingPermissionPromise.reject("PERMISSION_DENIED","Quyền VPN bị từ chối");}pendingPermissionPromise=null;pendingConfig=null;pendingServerName=null;}
    @ReactMethod public void stopVpn(Promise p){Intent i=new Intent(reactContext,XrayVpnService.class);i.setAction(XrayVpnService.ACTION_STOP);reactContext.startService(i);p.resolve(null);}
    @ReactMethod public void queryStats(Promise p){Intent i=new Intent(reactContext,XrayVpnService.class);i.setAction(XrayVpnService.ACTION_QUERY_STATS);reactContext.startService(i);p.resolve(null);}
    public static void sendStatusEvent(String s,String m){Map<String,Object> p=new HashMap<>();p.put("status",s);if(m!=null)p.put("msg",m);sendEvent("XrayVpnStatus",p);}
    public static void sendStatsEvent(long bI,long bO,long sI,long sO,long el){Map<String,Object> p=new HashMap<>();p.put("bytesIn",bI);p.put("bytesOut",bO);p.put("speedIn",sI);p.put("speedOut",sO);p.put("elapsedSeconds",el);sendEvent("XrayVpnStats",p);}
    @ReactMethod public void addListener(String e){}
    @ReactMethod public void removeListeners(Integer c){}
}
