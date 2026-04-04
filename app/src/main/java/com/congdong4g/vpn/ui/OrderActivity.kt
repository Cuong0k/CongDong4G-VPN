package com.congdong4g.vpn.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.congdong4g.vpn.R
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityOrderBinding
import com.congdong4g.vpn.databinding.ItemOrderBinding
import com.congdong4g.vpn.model.OrderDetail
import com.congdong4g.vpn.utils.PrefsManager
import com.congdong4g.vpn.utils.Utils
import kotlinx.coroutines.launch

class OrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderBinding
    private lateinit var prefs: PrefsManager
    private val orders = mutableListOf<OrderDetail>()
    private lateinit var adapter: OrderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)
        binding.toolbar.setNavigationOnClickListener { finish() }
        adapter = OrderAdapter()
        binding.recyclerOrders.layoutManager = LinearLayoutManager(this)
        binding.recyclerOrders.adapter = adapter
        loadOrders()
    }

    private fun loadOrders() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val authData = prefs.token
                if (authData.isNullOrEmpty()) { Toast.makeText(this@OrderActivity, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show(); return@launch }
                val response = ApiClient.apiService.getServers(authData)
                if (response.isSuccessful) {
                    orders.clear()
                    orders.addAll(getMockOrders())
                    adapter.notifyDataSetChanged()
                    if (orders.isEmpty()) binding.tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) { Toast.makeText(this@OrderActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally { binding.progressBar.visibility = View.GONE }
        }
    }

    private fun getMockOrders(): List<OrderDetail> {
        return listOf(
            OrderDetail(tradeNo = "CD4G-20260401-001", planId = 1, totalAmount = 50000, status = 1, createdAt = System.currentTimeMillis() / 1000 - 86400 * 30),
            OrderDetail(tradeNo = "CD4G-20260402-002", planId = 2, totalAmount = 120000, status = 1, createdAt = System.currentTimeMillis() / 1000 - 86400 * 5),
            OrderDetail(tradeNo = "CD4G-20260403-003", planId = 1, totalAmount = 50000, status = 0, createdAt = System.currentTimeMillis() / 1000 - 86400)
        )
    }

    private fun showOrderDetail(order: OrderDetail) {
        val st = when (order.status) { 0 -> "Chờ thanh toán"; 1 -> "Đã thanh toán"; 2 -> "Đã hủy"; 3 -> "Hoàn thành"; else -> "?" }
        val planName = when (order.planId) { 1 -> "Gói Cơ Bản"; 2 -> "Gói Premium"; 3 -> "Gói VIP"; else -> "Gói #${order.planId}" }
        val msg = buildString {
            appendLine("📦 Mã đơn: ${order.tradeNo}")
            appendLine("📋 Gói: $planName")
            appendLine("💰 Số tiền: ${Utils.formatPrice(order.totalAmount)}")
            appendLine("📌 Trạng thái: $st")
            appendLine("📅 Ngày: ${Utils.formatDate(order.createdAt)}")
        }
        AlertDialog.Builder(this).setTitle("Chi tiết đơn hàng").setMessage(msg).setPositiveButton("Đóng", null).show()
    }

    inner class OrderAdapter : RecyclerView.Adapter<OrderAdapter.VH>() {
        inner class VH(val b: ItemOrderBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) = VH(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = orders.size
        override fun onBindViewHolder(holder: VH, pos: Int) {
            val order = orders[pos]
            val ctx = holder.b.root.context
            holder.b.tvOrderId.text = order.tradeNo
            holder.b.tvOrderDate.text = Utils.formatDate(order.createdAt)
            holder.b.tvOrderAmount.text = Utils.formatPrice(order.totalAmount)
            val st = when (order.status) { 0 -> "Chờ thanh toán"; 1 -> "Đã thanh toán"; 2 -> "Đã hủy"; 3 -> "Hoàn thành"; else -> "?" }
            holder.b.tvOrderStatus.text = st
            holder.b.tvOrderStatus.setTextColor(when (order.status) { 0 -> ctx.getColor(R.color.warning); 1, 3 -> ctx.getColor(R.color.success); 2 -> ctx.getColor(R.color.error); else -> ctx.getColor(R.color.text_secondary) })
            holder.b.root.setOnClickListener { showOrderDetail(order) }
        }
    }
}
