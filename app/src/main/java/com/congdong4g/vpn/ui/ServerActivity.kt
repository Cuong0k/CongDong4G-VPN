package com.congdong4g.vpn.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.congdong4g.vpn.R
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityServerBinding
import com.congdong4g.vpn.databinding.ItemServerBinding
import com.congdong4g.vpn.model.Server
import com.congdong4g.vpn.utils.PrefsManager
import com.congdong4g.vpn.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class ServerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityServerBinding
    private lateinit var prefs: PrefsManager
    private val servers = mutableListOf<Server>()
    private val serverAdapter = ServerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerServers.layoutManager = LinearLayoutManager(this)
        binding.recyclerServers.adapter = serverAdapter
        loadServers()
    }

    private fun loadServers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val authData = prefs.token
                if (authData.isNullOrEmpty()) { Toast.makeText(this@ServerActivity, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show(); return@launch }
                val response = ApiClient.apiService.getServers(authData)
                if (response.isSuccessful && response.body()?.data != null) {
                    servers.clear()
                    servers.addAll(response.body()?.data ?: emptyList())
                    serverAdapter.notifyDataSetChanged()
                    if (servers.isEmpty()) binding.tvEmpty.visibility = View.VISIBLE
                    else pingAllServers()
                }
            } catch (e: Exception) { Toast.makeText(this@ServerActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally { binding.progressBar.visibility = View.GONE }
        }
    }

    private fun pingAllServers() {
        lifecycleScope.launch {
            servers.forEachIndexed { index, server ->
                val pingMs = pingServer(server)
                servers[index].ping = pingMs
                withContext(Dispatchers.Main) { serverAdapter.notifyItemChanged(index) }
            }
        }
    }

    private suspend fun pingServer(server: Server): Long {
        return withContext(Dispatchers.IO) {
            try {
                val start = System.currentTimeMillis()
                if (InetAddress.getByName(server.host).isReachable(3000)) System.currentTimeMillis() - start
                else pingViaTcp(server.host, server.port)
            } catch (e: Exception) { pingViaTcp(server.host, server.port) }
        }
    }

    private fun pingViaTcp(host: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { s -> s.connect(InetSocketAddress(host, if (port > 0) port else 443), 3000) }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            try { val start = System.currentTimeMillis()
                Socket().use { s -> s.connect(InetSocketAddress(host, 80), 3000) }
                System.currentTimeMillis() - start
            } catch (e2: Exception) { -1L }
        }
    }

    inner class ServerAdapter : RecyclerView.Adapter<ServerAdapter.VH>() {
        inner class VH(val b: ItemServerBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) = VH(ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = servers.size
        override fun onBindViewHolder(holder: VH, pos: Int) {
            val server = servers[pos]
            val ctx = holder.b.root.context
            holder.b.tvServerName.text = "${Utils.getCountryFlag(server.name)} ${server.name}"
            holder.b.tvServerHost.text = server.host
            holder.b.tvServerOnline.text = "${server.online} online"
            holder.b.tvServerType.text = server.type.uppercase()
            val pingText = if (server.ping >= 0) "${server.ping} ms" else "..."
            holder.b.tvPing.text = pingText
            holder.b.tvPing.setTextColor(when { server.ping < 0 -> ctx.getColor(R.color.text_secondary); server.ping < 50 -> ctx.getColor(R.color.success); server.ping < 150 -> ctx.getColor(R.color.warning); else -> ctx.getColor(R.color.error) })
            val sel = prefs.selectedServer
            holder.b.ivSelected.visibility = if (sel?.id == server.id) View.VISIBLE else View.GONE
            holder.b.cardServer.strokeWidth = if (sel?.id == server.id) 2 else 0
            holder.b.cardServer.strokeColor = ctx.getColor(R.color.primary)
            holder.b.root.setOnClickListener { prefs.selectedServer = server; Toast.makeText(ctx, "Đã chọn: ${server.name}", Toast.LENGTH_SHORT).show(); serverAdapter.notifyDataSetChanged(); finish() }
        }
    }
}
