package com.congdong4g.vpn;
import android.app.Activity;
import android.content.Intent;
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;
import com.congdong4g.vpn.modules.XrayVpnModule;
public class MainActivity extends ReactActivity {
    private static final int VPN_PERMISSION_REQ = 100;
    @Override protected String getMainComponentName(){return "CongDong4GVPN";}
    @Override protected ReactActivityDelegate createReactActivityDelegate(){
        return new DefaultReactActivityDelegate(this,getMainComponentName(),DefaultNewArchitectureEntryPoint.getFabricEnabled());
    }
    @Override public void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==VPN_PERMISSION_REQ){XrayVpnModule inst=getXrayModule();if(inst!=null)inst.onVpnPermissionResult(resultCode==Activity.RESULT_OK);}
    }
    private XrayVpnModule getXrayModule(){try{return(XrayVpnModule)getReactInstanceManager().getCurrentReactContext().getCatalystInstance().getNativeModule(XrayVpnModule.class);}catch(Exception e){return null;}}
}
