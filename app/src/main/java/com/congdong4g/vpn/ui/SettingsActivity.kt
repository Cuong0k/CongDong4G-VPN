package com.congdong4g.vpn.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.congdong4g.vpn.CongDong4GApp
import com.congdong4g.vpn.databinding.ActivitySettingsBinding
import com.congdong4g.vpn.databinding.ItemHistoryBinding
import com.congdong4g.vpn.model.ConnectionHistory
import com.congdong4g.vpn.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadHistory()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val prefs = CongDong4GApp.instance.prefsManager

        // Email
        binding.tvEmail.text = prefs.email ?: "Chưa đăng nhập"

        // Dark mode
        binding.switchDarkMode.isChecked = prefs.isDarkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Auto connect
        binding.switchAutoConnect.isChecked = prefs.isAutoConnect
        binding.switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            prefs.isAutoConnect = isChecked
        }

        // Clear history
        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử")
                .setMessage("Bạn có chắc muốn xóa toàn bộ lịch sử kết nối?")
                .setPositiveButton("Xóa") { _, _ ->
                    prefs.clearConnectionHistory()
                    loadHistory()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    prefs.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // Version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "Phiên bản ${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersion.text = "Phiên bản 1.0.0"
        }
    }

    private fun loadHistory() {
        val history = CongDong4GApp.instance.prefsManager.getConnectionHistory()

        if (history.isEmpty()) {
            binding.tvNoHistory.visibility = View.VISIBLE
            binding.recyclerHistory.visibility = View.GONE
        } else {
            binding.tvNoHistory.visibility = View.GONE
            binding.recyclerHistory.visibility = View.VISIBLE
            binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
            binding.recyclerHistory.adapter = HistoryAdapter(history)
        }
    }

    // History Adapter
    inner class HistoryAdapter(
        private val items: List<ConnectionHistory>
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val history = items[position]
            with(holder.binding) {
                val flag = Utils.getCountryFlag(history.serverName)
                tvServerName.text = "$flag ${history.serverName}"

                val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                tvConnectedAt.text = sdf.format(Date(history.connectedAt))

                tvDuration.text = Utils.formatDuration(history.duration)

                val totalTraffic = history.upload + history.download
                tvTraffic.text = Utils.formatBytes(totalTraffic)
            }
        }

        override fun getItemCount() = items.size
    }
}
