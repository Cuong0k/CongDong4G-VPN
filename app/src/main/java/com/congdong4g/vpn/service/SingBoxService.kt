package com.congdong4g.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.congdong4g.vpn.R
import com.congdong4g.vpn.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class SingBoxService : VpnService() {

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_CONFIG = "config"
        
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1
        
        var isRunning = false
        var uploadBytes = 0L
        var downloadBytes = 0L
        var connectedTime = 0L
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isActive = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val config = intent.getStringExtra(EXTRA_CONFIG) ?: ""
                startVpn(config)
            }
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(config: String) {
        if (isRunning) return
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Đang kết nối..."))

        try {
            // Parse config to get server info
            val serverInfo = parseConfig(config)
            
            // Build VPN interface
            val builder = Builder()
                .setSession("CongDong4G VPN")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .setMtu(1500)
                .setBlocking(true)

            // Allow bypass for some apps
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                isRunning = true
                isActive = true
                connectedTime = System.currentTimeMillis()
                uploadBytes = 0L
                downloadBytes = 0L
                
                updateNotification("Đã kết nối - ${serverInfo.name}")
                
                // Start packet forwarding
                scope.launch {
                    runTunnel(serverInfo)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private suspend fun runTunnel(serverInfo: ServerInfo) {
        val vpnFd = vpnInterface?.fileDescriptor ?: return
        val input = FileInputStream(vpnFd)
        val output = FileOutputStream(vpnFd)
        
        try {
            // Create UDP channel to proxy server
            val tunnel = DatagramChannel.open()
            tunnel.configureBlocking(false)
            protect(tunnel.socket()) // Protect socket from VPN routing
            tunnel.connect(InetSocketAddress(serverInfo.host, serverInfo.port))

            val packet = ByteBuffer.allocate(32767)
            
            while (isActive) {
                // Read from TUN
                packet.clear()
                val length = input.read(packet.array())
                
                if (length > 0) {
                    packet.limit(length)
                    
                    // Forward to server
                    tunnel.write(packet)
                    uploadBytes += length
                }
                
                // Read from server
                packet.clear()
                val received = tunnel.read(packet)
                
                if (received > 0) {
                    packet.flip()
                    output.write(packet.array(), 0, received)
                    downloadBytes += received
                }
                
                delay(1) // Small delay to prevent CPU spinning
            }
            
            tunnel.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        input.close()
        output.close()
    }

    private fun stopVpn() {
        isActive = false
        isRunning = false
        
        scope.cancel()
        
        vpnInterface?.close()
        vpnInterface = null
        
        stopForeground(true)
        stopSelf()
    }

    private fun parseConfig(config: String): ServerInfo {
        // Parse VMess/VLESS/Trojan/Shadowsocks URL
        return when {
            config.startsWith("vmess://") -> parseVmess(config)
            config.startsWith("vless://") -> parseVless(config)
            config.startsWith("trojan://") -> parseTrojan(config)
            config.startsWith("ss://") -> parseShadowsocks(config)
            else -> ServerInfo("Unknown", "127.0.0.1", 443, "vmess")
        }
    }

    private fun parseVmess(url: String): ServerInfo {
        try {
            val encoded = url.removePrefix("vmess://")
            val decoded = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
            val json = org.json.JSONObject(decoded)
            return ServerInfo(
                name = json.optString("ps", "VMess Server"),
                host = json.optString("add", ""),
                port = json.optInt("port", 443),
                protocol = "vmess"
            )
        } catch (e: Exception) {
            return ServerInfo("VMess", "127.0.0.1", 443, "vmess")
        }
    }

    private fun parseVless(url: String): ServerInfo {
        try {
            val uri = android.net.Uri.parse(url)
            return ServerInfo(
                name = uri.fragment ?: "VLESS Server",
                host = uri.host ?: "",
                port = uri.port,
                protocol = "vless"
            )
        } catch (e: Exception) {
            return ServerInfo("VLESS", "127.0.0.1", 443, "vless")
        }
    }

    private fun parseTrojan(url: String): ServerInfo {
        try {
            val uri = android.net.Uri.parse(url)
            return ServerInfo(
                name = uri.fragment ?: "Trojan Server",
                host = uri.host ?: "",
                port = uri.port,
                protocol = "trojan"
            )
        } catch (e: Exception) {
            return ServerInfo("Trojan", "127.0.0.1", 443, "trojan")
        }
    }

    private fun parseShadowsocks(url: String): ServerInfo {
        try {
            val uri = android.net.Uri.parse(url)
            return ServerInfo(
                name = uri.fragment ?: "SS Server",
                host = uri.host ?: "",
                port = uri.port,
                protocol = "shadowsocks"
            )
        } catch (e: Exception) {
            return ServerInfo("Shadowsocks", "127.0.0.1", 443, "shadowsocks")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CongDong4G VPN")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    data class ServerInfo(
        val name: String,
        val host: String,
        val port: Int,
        val protocol: String
    )
}
