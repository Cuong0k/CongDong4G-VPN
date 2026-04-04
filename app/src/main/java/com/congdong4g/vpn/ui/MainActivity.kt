package com.congdong4g.vpn.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.R
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityMainBinding
import com.congdong4g.vpn.model.Server
import com.congdong4g.vpn.service.SingBoxService
import com.congdong4g.vpn.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager

    private var servers = listOf<Server>()
    private var selectedServer: Server? = null
    private var subscribeUrl: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 1000)
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpn()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setupUI()
        setupClickListeners()
        loadUserInfo()
        loadServers()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        if (SingBoxService.isRunning) {
            handler.post(updateRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun setupUI() {
        updateUI()
    }

    private fun updateUI() {
        if (SingBoxService.isRunning) {
            binding.tvStatus.text = "Đã kết nối"
            binding.ivStatus.setImageResource(R.drawable.ic_shield_on)
            binding.btnConnect.text = "NGẮT KẾT NỐI"
            binding.btnConnect.setBackgroundColor(getColor(R.color.error))
        } else {
            binding.tvStatus.text = "Chưa kết nối"
            binding.ivStatus.setImageResource(R.drawable.ic_shield_off)
            binding.btnConnect.text = "KẾT NỐI"
            binding.btnConnect.setBackgroundColor(getColor(R.color.primary))
        }
    }

    private fun updateStats() {
        if (SingBoxService.isRunning) {
            binding.tvUpload.text = formatBytes(SingBoxService.uploadBytes)
            binding.tvDownload.text = formatBytes(SingBoxService.downloadBytes)

            val duration = (System.currentTimeMillis() - SingBoxService.connectedTime) / 1000
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60
            binding.tvTime.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            if (SingBoxService.isRunning) {
                stopVpn()
            } else {
                prepareVpn()
            }
        }

        binding.cardServer.setOnClickListener {
            showServerDialog()
        }

        binding.btnRefreshServer.setOnClickListener {
            refreshServers()
        }

        binding.navPlan.setOnClickListener {
            startActivity(Intent(this, PlanActivity::class.java))
        }

        binding.navSpeed.setOnClickListener {
            startActivity(Intent(this, SpeedTestActivity::class.java))
        }

        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun refreshServers() {
        val rotation = AnimationUtils.loadAnimation(this, R.anim.rotate)
        binding.btnRefreshServer.startAnimation(rotation)
        Toast.makeText(this, "Đang cập nhật máy chủ...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val authData = prefs.token ?: return@launch
                val response = ApiClient.apiService.getServers(authData)

                if (response.isSuccessful && response.body()?.data != null) {
                    servers = response.body()?.data ?: emptyList()
                    updateServerUI()
                    Toast.makeText(this@MainActivity, "Đã cập nhật ${servers.size} máy chủ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Không thể cập nhật", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnRefreshServer.clearAnimation()
            }
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val authData = prefs.token ?: return@launch

                val userResponse = ApiClient.apiService.getUserInfo(authData)
                if (userResponse.isSuccessful && userResponse.body()?.data != null) {
                    val user = userResponse.body()?.data!!
                    val used = user.upload + user.download
                    val total = user.transferEnable
                    binding.tvDataUsed.text = "${formatBytes(used)} / ${formatBytes(total)}"

                    val expiredAt = user.expiredAt ?: 0L
                    if (expiredAt > 0) {
                        val daysLeft = ((expiredAt * 1000 - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                        binding.tvDaysLeft.text = "Còn $daysLeft ngày"
                    }

                    binding.progressData.max = 100
                    binding.progressData.progress = if (total > 0) ((used * 100) / total).toInt() else 0
                }

                val subResponse = ApiClient.apiService.getSubscription(authData)
                if (subResponse.isSuccessful && subResponse.body()?.data != null) {
                    subscribeUrl = subResponse.body()?.data?.subscribeUrl
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadServers() {
        lifecycleScope.launch {
            try {
                val authData = prefs.token ?: return@launch
                val response = ApiClient.apiService.getServers(authData)
                if (response.isSuccessful && response.body()?.data != null) {
                    servers = response.body()?.data ?: emptyList()
                    if (servers.isNotEmpty()) {
                        selectedServer = servers[0]
                    }
                    updateServerUI()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateServerUI() {
        binding.tvServerCount.text = "${servers.size} máy chủ"
        selectedServer?.let { server ->
            binding.tvServerName.text = server.name
        } ?: run {
            binding.tvServerName.text = "Chọn máy chủ"
        }
    }

    private fun showServerDialog() {
        if (servers.isEmpty()) {
            Toast.makeText(this, "Đang tải danh sách server...", Toast.LENGTH_SHORT).show()
            loadServers()
            return
        }

        val serverNames = servers.map { "${it.name} (${it.online} online)" }.toTypedArray()
        val currentIndex = servers.indexOf(selectedServer).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Chọn máy chủ (${servers.size})")
            .setSingleChoiceItems(serverNames, currentIndex) { dialog, which ->
                selectedServer = servers[which]
                updateServerUI()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun prepareVpn() {
        if (selectedServer == null) {
            Toast.makeText(this, "Vui lòng chọn máy chủ", Toast.LENGTH_SHORT).show()
            return
        }

        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        lifecycleScope.launch {
            try {
                binding.btnConnect.isEnabled = false
                binding.tvStatus.text = "Đang kết nối..."

                val config = getServerConfig()

                if (config.isNotEmpty()) {
                    val intent = Intent(this@MainActivity, SingBoxService::class.java)
                    intent.action = SingBoxService.ACTION_START
                    intent.putExtra(SingBoxService.EXTRA_CONFIG, config)
                    startService(intent)

                    kotlinx.coroutines.delay(1000)
                    updateUI()
                    handler.post(updateRunnable)
                } else {
                    Toast.makeText(this@MainActivity, "Không lấy được config", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnConnect.isEnabled = true
            }
        }
    }

    private suspend fun getServerConfig(): String {
        return withContext(Dispatchers.IO) {
            try {
                val authData = prefs.token ?: return@withContext ""
                val resp = ApiClient.apiService.getSubscribeConfig(authData)
                if (!resp.isSuccessful || resp.body()?.data == null) return@withContext ""
                val subscribeContent = resp.body()?.data?.subscribe ?: ""
                if (subscribeContent.isBlank()) return@withContext ""
                val decoded = try {
                    String(android.util.Base64.decode(subscribeContent.trim(), android.util.Base64.DEFAULT))
                } catch (e: Exception) {
                    subscribeContent
                }
                val lines = decoded.split("\n").filter { it.isNotBlank() }
                selectedServer?.let { server ->
                    lines.find { line -> line.contains(server.name, true) || line.contains(server.host, true) }?.let { return@withContext it.trim() }
                }
                lines.firstOrNull()?.trim() ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    private fun stopVpn() {
        val intent = Intent(this, SingBoxService::class.java)
        intent.action = SingBoxService.ACTION_STOP
        startService(intent)

        handler.removeCallbacks(updateRunnable)
        handler.postDelayed({
            updateUI()
            binding.tvUpload.text = "0 B"
            binding.tvDownload.text = "0 B"
            binding.tvTime.text = "00:00:00"
        }, 500)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
