package com.congdong4g.vpn.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private var serverConfigs = mutableMapOf<Int, String>() // server id -> config url
    
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

    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val token = prefs.token ?: return@launch

                val userResponse = ApiClient.apiService.getUserInfo("Bearer $token")
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

                val subResponse = ApiClient.apiService.getSubscription("Bearer $token")
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
                val token = prefs.token ?: return@launch
                val response = ApiClient.apiService.getServers("Bearer $token")
                if (response.isSuccessful && response.body()?.data != null) {
                    servers = response.body()?.data ?: emptyList()
                    if (servers.isNotEmpty()) {
                        selectedServer = servers[0]
                        updateServerUI()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateServerUI() {
        selectedServer?.let { server ->
            binding.tvServerName.text = server.name
        }
    }

    private fun showServerDialog() {
        if (servers.isEmpty()) {
            Toast.makeText(this, "Đang tải danh sách server...", Toast.LENGTH_SHORT).show()
            loadServers()
            return
        }

        val serverNames = servers.map { it.name }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chọn máy chủ")
            .setItems(serverNames) { _, which ->
                selectedServer = servers[which]
                updateServerUI()
            }
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
                
                // Get server config from subscription
                val config = getServerConfig()
                
                if (config.isNotEmpty()) {
                    val intent = Intent(this@MainActivity, SingBoxService::class.java)
                    intent.action = SingBoxService.ACTION_START
                    intent.putExtra(SingBoxService.EXTRA_CONFIG, config)
                    startService(intent)
                    
                    // Wait a bit and update UI
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
                val url = subscribeUrl ?: return@withContext ""
                val content = URL(url).readText()
                
                // Decode base64 if needed
                val decoded = try {
                    String(android.util.Base64.decode(content, android.util.Base64.DEFAULT))
                } catch (e: Exception) {
                    content
                }
                
                // Get first server config
                val lines = decoded.split("\n").filter { it.isNotEmpty() }
                
                // Find config matching selected server
                selectedServer?.let { server ->
                    for (line in lines) {
                        if (line.contains(server.name) || line.contains(server.host)) {
                            return@withContext line.trim()
                        }
                    }
                }
                
                // Return first config if no match
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
