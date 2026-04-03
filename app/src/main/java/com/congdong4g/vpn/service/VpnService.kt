package com.congdong4g.vpn.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.congdong4g.vpn.CongDong4GApp
import com.congdong4g.vpn.R
import com.congdong4g.vpn.model.ConnectionHistory
import com.congdong4g.vpn.model.Server
import com.congdong4g.vpn.model.VpnStatus
import com.congdong4g.vpn.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream

class VpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.congdong4g.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.congdong4g.vpn.DISCONNECT"
        const val EXTRA_SERVER = "extra_server"
        
        var status: VpnStatus = VpnStatus.DISCONNECTED
            private set
        
        var currentServer: Server? = null
            private set
        
        var connectedTime: Long = 0
            private set
        
        var uploadBytes: Long = 0
            private set
        
        var downloadBytes: Long = 0
            private set

        val statusListeners = mutableListOf<(VpnStatus) -> Unit>()
        val trafficListeners = mutableListOf<(Long, Long) -> Unit>()
        
        fun addStatusListener(listener: (VpnStatus) -> Unit) {
            statusListeners.add(listener)
        }
        
        fun removeStatusListener(listener: (VpnStatus) -> Unit) {
            statusListeners.remove(listener)
        }

        fun addTrafficListener(listener: (Long, Long) -> Unit) {
            trafficListeners.add(listener)
        }

        fun removeTrafficListener(listener: (Long, Long) -> Unit) {
            trafficListeners.remove(listener)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverJson = intent.getStringExtra(EXTRA_SERVER)
                // Parse server and connect
                startVpn()
            }
            ACTION_DISCONNECT -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        updateStatus(VpnStatus.CONNECTING)
        
        try {
            // Create VPN interface
            val builder = Builder()
                .setSession("CongDong4G VPN")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                connectedTime = System.currentTimeMillis()
                uploadBytes = 0
                downloadBytes = 0
                
                startForeground(1, createNotification())
                updateStatus(VpnStatus.CONNECTED)
                
                // Start traffic monitoring
                startTrafficMonitor()
            } else {
                updateStatus(VpnStatus.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateStatus(VpnStatus.ERROR)
        }
    }

    private fun stopVpn() {
        updateStatus(VpnStatus.DISCONNECTING)
        
        job?.cancel()
        
        // Save connection history
        if (connectedTime > 0) {
            val history = ConnectionHistory(
                id = System.currentTimeMillis(),
                serverName = currentServer?.name ?: "Unknown",
                serverHost = currentServer?.host ?: "",
                connectedAt = connectedTime,
                disconnectedAt = System.currentTimeMillis(),
                upload = uploadBytes,
                download = downloadBytes,
                duration = (System.currentTimeMillis() - connectedTime) / 1000
            )
            CongDong4GApp.instance.prefsManager.addConnectionHistory(history)
        }
        
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        connectedTime = 0
        uploadBytes = 0
        downloadBytes = 0
        currentServer = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        updateStatus(VpnStatus.DISCONNECTED)
        stopSelf()
    }

    private fun startTrafficMonitor() {
        job = scope.launch {
            while (isActive && status == VpnStatus.CONNECTED) {
                // Simulate traffic (replace with real sing-box stats)
                uploadBytes += (1024..10240).random()
                downloadBytes += (2048..20480).random()
                
                withContext(Dispatchers.Main) {
                    trafficListeners.forEach { it(uploadBytes, downloadBytes) }
                }
                
                delay(1000)
            }
        }
    }

    private fun updateStatus(newStatus: VpnStatus) {
        status = newStatus
        statusListeners.forEach { it(newStatus) }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, VpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CongDong4GApp.CHANNEL_ID)
            .setContentTitle("CongDong4G VPN")
            .setContentText("Đang kết nối: ${currentServer?.name ?: "Server"}")
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "Ngắt kết nối", disconnectPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        scope.cancel()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}
