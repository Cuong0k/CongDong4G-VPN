package com.congdong4g.vpn.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityPlanBinding
import com.congdong4g.vpn.databinding.ItemPlanBinding
import com.congdong4g.vpn.model.Plan
import com.congdong4g.vpn.utils.PrefsManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanBinding
    private lateinit var prefs: PrefsManager
    private val plans = mutableListOf<Plan>()
    private lateinit var adapter: PlanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = PlanAdapter()
        binding.recyclerPlans.layoutManager = LinearLayoutManager(this)
        binding.recyclerPlans.adapter = adapter
        
        loadPlans()
    }

    private fun loadPlans() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val token = prefs.token
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@PlanActivity, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val response = ApiClient.apiService.getPlans("Bearer $token")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.data != null && body.data.isNotEmpty()) {
                        plans.clear()
                        plans.addAll(body.data)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@PlanActivity, "Không có gói cước nào", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PlanActivity, "Lỗi: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PlanActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun formatPrice(price: Int?): String {
        if (price == null || price == 0) return "Miễn phí"
        val priceVND = price / 100
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        return "Từ ${formatter.format(priceVND)} đ"
    }

    private fun formatData(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> String.format("%.0f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.0f MB", bytes / 1_048_576.0)
        else -> "$bytes B"
    }

    inner class PlanAdapter : RecyclerView.Adapter<PlanAdapter.VH>() {
        
        inner class VH(val b: ItemPlanBinding) : RecyclerView.ViewHolder(b.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        
        override fun getItemCount() = plans.size
        
        override fun onBindViewHolder(holder: VH, position: Int) {
            val plan = plans[position]
            holder.b.tvPlanName.text = plan.name
            holder.b.tvPlanData.text = formatData(plan.transferEnable)
            holder.b.tvPlanDevice.text = "${plan.deviceLimit ?: 1} thiết bị"
            holder.b.tvPlanPrice.text = formatPrice(plan.monthPrice)
            
            holder.b.root.setOnClickListener {
                val intent = Intent(this@PlanActivity, PaymentActivity::class.java)
                intent.putExtra("plan_id", plan.id)
                intent.putExtra("plan_name", plan.name)
                intent.putExtra("plan_price", (plan.monthPrice ?: 0).toLong() / 100)
                intent.putExtra("period", "month_price")
                startActivity(intent)
            }
        }
    }
}
