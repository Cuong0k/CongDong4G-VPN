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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = PlanAdapter()
        
        loadPlans()
    }
    
    private fun loadPlans() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val token = prefs.getToken() ?: ""
                val response = ApiClient.apiService.getPlanList("Bearer $token")
                
                if (response.isSuccessful && response.body()?.data != null) {
                    plans.clear()
                    plans.addAll(response.body()?.data ?: emptyList())
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PlanActivity, "Lỗi tải gói cước", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun formatPrice(price: Long): String {
        // API trả về đơn vị xu, chia 100 để ra VND
        val priceVND = price / 100
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        return "${formatter.format(priceVND)} đ"
    }
    
    private fun formatData(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    inner class PlanAdapter : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {
        
        inner class PlanViewHolder(val binding: ItemPlanBinding) : RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
            val binding = ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PlanViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
            val plan = plans[position]
            holder.binding.apply {
                tvPlanName.text = plan.name
                tvData.text = formatData(plan.transfer_enable ?: 0)
                tvDevices.text = "${plan.device_limit ?: 2} thiết bị"
                tvPrice.text = "Từ ${formatPrice(plan.month_price ?: 0)}"
                
                root.setOnClickListener {
                    val intent = Intent(this@PlanActivity, PaymentActivity::class.java)
                    intent.putExtra("plan_id", plan.id)
                    intent.putExtra("plan_name", plan.name)
                    intent.putExtra("plan_price", (plan.month_price ?: 0) / 100)
                    intent.putExtra("period", "month_price")
                    startActivity(intent)
                }
            }
        }
        
        override fun getItemCount() = plans.size
    }
}
