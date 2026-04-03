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
        binding.recyclerPlans.layoutManager = LinearLayoutManager(this)
        binding.recyclerPlans.adapter = PlanAdapter()
        loadPlans()
    }

    private fun loadPlans() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val token = prefs.token ?: ""
                val response = ApiClient.apiService.getPlans("Bearer $token")
                if (response.isSuccessful && response.body()?.data != null) {
                    plans.clear()
                    plans.addAll(response.body()?.data ?: emptyList())
                    binding.recyclerPlans.adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlanActivity, "Lỗi tải gói cước", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun formatPrice(price: Int): String {
        return "Từ ${NumberFormat.getInstance(Locale("vi", "VN")).format(price / 100)} đ"
    }

    private fun formatData(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> String.format("%.0f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.0f MB", bytes / 1_048_576.0)
        else -> "$bytes B"
    }

    inner class PlanAdapter : RecyclerView.Adapter<PlanAdapter.VH>() {
        inner class VH(val b: ItemPlanBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = plans.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val plan = plans[position]
            holder.b.tvPlanName.text = plan.name
            holder.b.tvPlanData.text = formatData(plan.transferEnable)
            holder.b.tvPlanDevice.text = "${plan.deviceLimit ?: 2} thiết bị"
            holder.b.tvPlanPrice.text = formatPrice(plan.monthPrice ?: 0)
            holder.b.root.setOnClickListener {
                startActivity(Intent(this@PlanActivity, PaymentActivity::class.java).apply {
                    putExtra("plan_id", plan.id)
                    putExtra("plan_name", plan.name)
                    putExtra("plan_price", (plan.monthPrice ?: 0).toLong() / 100)
                    putExtra("period", "month_price")
                })
            }
        }
    }
}
