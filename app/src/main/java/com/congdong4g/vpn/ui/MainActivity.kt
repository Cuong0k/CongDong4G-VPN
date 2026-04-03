package com.congdong4g.vpn.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.CongDong4GApp
import com.congdong4g.vpn.R
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityMainBinding
import com.congdong4g.vpn.model.Server
import com.congdong4g.vpn.model.UserInfo
import com.congdong4g.vpn.model.VpnStatus
import com.congdong4g.vpn.service.VpnService as AppVpnService
import com.congdong4g.vpn.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var servers: List<Server> = emptyList()
    private var selectedServer: Server? = null
    private var userInfo: UserInfo? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnConnection()
        } else {
            Toast.makeText(this, "Cần quyền VPN để kết nối", Toast.LENGTH_SHORT).show()
        }
    }

    private val statusListener: (VpnStatus) -> Unit = { status ->
        runOnUiThread { updateVpnStatus(status) }
    }

    private val trafficListener: (Long, Long) -> Unit = { upload, download ->
        runOnUiThread { updateTraffic(upload, download) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadUserInfo()
        loadServers()

        AppVpnService.addStatusListener(statusListener)
        AppVpnService.addTrafficListener(trafficListener)
        updateVpnStatus(AppVpnService.status)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppVpnService.removeStatusListener(statusListener)
        AppVpnService.removeTrafficListener(trafficListener)
    }

    private fun setupViews() {
        // Connect button
        binding.btnConnect.setOnClickListener {
            when (AppVpnService.status) {
                VpnStatus.DISCONNECTED, VpnStatus.ERROR -> {
                    if (selectedServer == null) {
                        Toast.makeText(this, "Vui lòng chọn server", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    requestVpnPermission()
                }
                VpnStatus.CONNECTED -> {
                    disconnectVpn()
                }
                else -> {}
            }
        }

        // Server selection
        binding.cardServer.setOnClickListener {
            showServerDialog()
        }

        // Bottom navigation
        binding.navPlan.setOnClickListener {
            startActivity(Intent(this, PlanActivity::class.java))
        }

        binding.navSpeedTest.setOnClickListener {
            startActivity(Intent(this, SpeedTestActivity::class.java))
        }

        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Restore selected server
        selectedServer = CongDong4GApp.instance.prefsManager.selectedServer
        updateServerDisplay()
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val token = CongDong4GApp.instance.prefsManager.token ?: return@launch
                val response = ApiClient.apiService.getUserInfo("$token")

                if (response.isSuccessful && response.body()?.data != null) {
                    userInfo = response.body()?.data
                    updateUserInfoDisplay()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUserInfoDisplay() {
        userInfo?.let { info ->
            val used = info.upload + info.download
            val total = info.transferEnable
            val percentage = Utils.calculateDataPercentage(used, total)

            binding.tvDataUsed.text = "${Utils.formatBytes(used)} / ${Utils.formatBytes(total)}"
            binding.progressData.progress = percentage

            val remainingDays = Utils.getRemainingDays(info.expiredAt)
            binding.tvExpiry.text = when {
                info.expiredAt == null -> "Không giới hạn"
                remainingDays <= 0 -> "Đã hết hạn"
                else -> "Còn $remainingDays ngày"
            }

            // Show warning if low data or expiring soon
            if (percentage > 90 || (remainingDays in 1..3)) {
                binding.cardWarning.visibility = View.VISIBLE
                binding.tvWarning.text = when {
                    percentage > 90 -> "Dữ liệu sắp hết, vui lòng gia hạn!"
                    else -> "Gói sắp hết hạn, vui lòng gia hạn!"
                }
            }
        }
    }

    private fun loadServers() {
        lifecycleScope.launch {
            try {
                val token = CongDong4GApp.instance.prefsManager.token ?: return@launch
                val response = ApiClient.apiService.getServers("$token")

                if (response.isSuccessful && response.body()?.data != null) {
                    servers = response.body()?.data ?: emptyList()

                    // Auto select first server if none selected
                    if (selectedServer == null && servers.isNotEmpty()) {
                        selectedServer = servers.first()
                        CongDong4GApp.instance.prefsManager.selectedServer = selectedServer
                        updateServerDisplay()
                    }

                    // Ping servers
                    pingServers()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pingServers() {
        lifecycleScope.launch {
            servers.forEach { server ->
                try {
                    val startTime = System.currentTimeMillis()
                    val process = Runtime.getRuntime().exec("ping -c 1 -W 2 ${server.host}")
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        server.ping = System.currentTimeMillis() - startTime
                    }
                } catch (e: Exception) {
                    server.ping = -1
                }
            }
            // Sort by ping
            servers = servers.sortedBy { if (it.ping < 0) Long.MAX_VALUE else it.ping }
        }
    }

    private fun showServerDialog() {
        if (servers.isEmpty()) {
            Toast.makeText(this, "Đang tải danh sách server...", Toast.LENGTH_SHORT).show()
            return
        }

        val serverNames = servers.map { server ->
            val flag = Utils.getCountryFlag(server.name)
            val ping = if (server.ping > 0) " (${server.ping}ms)" else ""
            "$flag ${server.name}$ping"
        }.toTypedArray()

        val currentIndex = servers.indexOfFirst { it.id == selectedServer?.id }

        AlertDialog.Builder(this)
            .setTitle("Chọn Server")
            .setSingleChoiceItems(serverNames, currentIndex) { dialog, which ->
                selectedServer = servers[which]
                CongDong4GApp.instance.prefsManager.selectedServer = selectedServer
                updateServerDisplay()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateServerDisplay() {
        selectedServer?.let { server ->
            val flag = Utils.getCountryFlag(server.name)
            binding.tvServerName.text = "$flag ${server.name}"
            val ping = if (server.ping > 0) "${server.ping}ms" else "---"
            binding.tvServerPing.text = ping
        } ?: run {
            binding.tvServerName.text = "Chọn server"
            binding.tvServerPing.text = "---"
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnConnection()
        }
    }

    private fun startVpnConnection() {
        val intent = Intent(this, AppVpnService::class.java).apply {
            action = AppVpnService.ACTION_CONNECT
            putExtra(AppVpnService.EXTRA_SERVER, Gson().toJson(selectedServer))
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun disconnectVpn() {
        val intent = Intent(this, AppVpnService::class.java).apply {
            action = AppVpnService.ACTION_DISCONNECT
        }
        startService(intent)
    }

    private fun updateVpnStatus(status: VpnStatus) {
        when (status) {
            VpnStatus.DISCONNECTED -> {
                binding.btnConnect.text = "KẾT NỐI"
                binding.btnConnect.setBackgroundColor(getColor(R.color.primary))
                binding.tvStatus.text = "Chưa kết nối"
                binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
                binding.ivStatus.setImageResource(R.drawable.ic_shield_off)
                binding.cardTraffic.visibility = View.GONE
            }
            VpnStatus.CONNECTING -> {
                binding.btnConnect.text = "ĐANG KẾT NỐI..."
                binding.btnConnect.isEnabled = false
                binding.tvStatus.text = "Đang kết nối..."
                binding.tvStatus.setTextColor(getColor(R.color.warning))
            }
            VpnStatus.CONNECTED -> {
                binding.btnConnect.text = "NGẮT KẾT NỐI"
                binding.btnConnect.isEnabled = true
                binding.btnConnect.setBackgroundColor(getColor(R.color.error))
                binding.tvStatus.text = "Đã kết nối"
                binding.tvStatus.setTextColor(getColor(R.color.success))
                binding.ivStatus.setImageResource(R.drawable.ic_shield_on)
                binding.cardTraffic.visibility = View.VISIBLE
            }
            VpnStatus.DISCONNECTING -> {
                binding.btnConnect.text = "ĐANG NGẮT..."
                binding.btnConnect.isEnabled = false
                binding.tvStatus.text = "Đang ngắt kết nối..."
            }
            VpnStatus.ERROR -> {
                binding.btnConnect.text = "KẾT NỐI"
                binding.btnConnect.isEnabled = true
                binding.btnConnect.setBackgroundColor(getColor(R.color.primary))
                binding.tvStatus.text = "Lỗi kết nối"
                binding.tvStatus.setTextColor(getColor(R.color.error))
            }
        }
    }

    private fun updateTraffic(upload: Long, download: Long) {
        binding.tvUpload.text = Utils.formatBytes(upload)
        binding.tvDownload.text = Utils.formatBytes(download)

        if (AppVpnService.connectedTime > 0) {
            val duration = (System.currentTimeMillis() - AppVpnService.connectedTime) / 1000
            binding.tvDuration.text = Utils.formatDuration(duration)
        }
    }
}
