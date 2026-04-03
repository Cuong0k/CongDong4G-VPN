package com.congdong4g.vpn.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivitySettingsBinding
import com.congdong4g.vpn.utils.PrefsManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)
        
        setupUI()
        setupClickListeners()
        loadUserInfo()
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.tvEmail.text = prefs.getEmail() ?: ""
        binding.switchDarkMode.isChecked = prefs.isDarkMode()
        binding.switchAutoConnect.isChecked = prefs.isAutoConnect()
        
        binding.tvVersion.text = "Phiên bản 1.0.0"
    }
    
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val token = prefs.getToken() ?: ""
                val response = ApiClient.apiService.getUserInfo("Bearer $token")
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val user = response.body()?.data!!
                    
                    binding.tvPlanName.visibility = View.VISIBLE
                    binding.tvPlanName.text = "Gói: ${user.plan_id ?: "Chưa có"}"
                    
                    binding.tvExpireDate.visibility = View.VISIBLE
                    val expireTime = user.expired_at ?: 0
                    if (expireTime > 0) {
                        val date = Date(expireTime * 1000L)
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        binding.tvExpireDate.text = "Hết hạn: ${formatter.format(date)}"
                    } else {
                        binding.tvExpireDate.text = "Hết hạn: Chưa xác định"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.setDarkMode(isChecked)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        
        binding.switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAutoConnect(isChecked)
        }
        
        binding.btnClearHistory.setOnClickListener {
            prefs.clearConnectionHistory()
            Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                prefs.clearAll()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
