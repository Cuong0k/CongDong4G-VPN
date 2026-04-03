package com.congdong4g.vpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.congdong4g.vpn.utils.PrefsManager

class CongDong4GApp : Application() {

    companion object {
        lateinit var instance: CongDong4GApp
            private set
        
        const val CHANNEL_ID = "vpn_service"
        const val CHANNEL_NAME = "VPN Service"
    }

    lateinit var prefsManager: PrefsManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefsManager = PrefsManager(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN Connection Status"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
