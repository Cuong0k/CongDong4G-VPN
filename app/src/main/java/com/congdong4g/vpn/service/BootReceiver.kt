package com.congdong4g.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.congdong4g.vpn.CongDong4GApp

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = CongDong4GApp.instance.prefsManager
            
            // Check if auto connect is enabled and user is logged in
            if (prefs.isAutoConnect && prefs.isLoggedIn()) {
                val lastServer = prefs.lastConnectedServer ?: prefs.selectedServer
                
                if (lastServer != null) {
                    val serviceIntent = Intent(context, VpnService::class.java).apply {
                        action = VpnService.ACTION_CONNECT
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        }
    }
}
