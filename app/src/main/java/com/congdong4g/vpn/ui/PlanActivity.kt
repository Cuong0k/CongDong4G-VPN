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
import com.congdong4g.vpn.CongDong4GApp
import com.congdong4g.vpn.R
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityPlanBinding
import com.congdong4g.vpn.databinding.ItemPlanBinding
import com.congdong4g.vpn.model.Plan
import com.congdong4g.vpn.utils.Utils
import kotlinx.coroutines.launch

class PlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanBinding
    private val plans = mutableListOf<Plan>()
    private lateinit var adapter: PlanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadPlans()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = PlanAdapter(plans) { plan ->
            showPlanDetail(plan)
        }
        binding.recyclerPlans.layoutManager = LinearLayoutManager(this)
        binding.recyclerPlans.adapter = adapter
    }

    private fun loadPlans() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val token = CongDong4GApp.instance.prefsManager.token ?: return@launch
                val response = ApiClient.apiService.getPlans("$token")

                if (response.isSuccessful && response.body()?.data != null) {
                    plans.clear()
                    plans.addAll(response.body()?.data ?: emptyList())
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PlanActivity, "Lỗi tải danh sách gói", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showPlanDetail(plan: Plan) {
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra("plan_id", plan.id)
        intent.putExtra("plan_name", plan.name)
        intent.putExtra("plan_month_price", plan.monthPrice ?: 0)
        intent.putExtra("plan_quarter_price", plan.quarterPrice ?: 0)
        intent.putExtra("plan_half_year_price", plan.halfYearPrice ?: 0)
        intent.putExtra("plan_year_price", plan.yearPrice ?: 0)
        intent.putExtra("plan_transfer", plan.transferEnable)
        intent.putExtra("plan_device_limit", plan.deviceLimit ?: 0)
        startActivity(intent)
    }

    // Adapter
    inner class PlanAdapter(
        private val items: List<Plan>,
        private val onClick: (Plan) -> Unit
    ) : RecyclerView.Adapter<PlanAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemPlanBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val plan = items[position]
            with(holder.binding) {
                tvPlanName.text = plan.name
                tvPlanData.text = Utils.formatBytes(plan.transferEnable)
                
                val deviceText = if (plan.deviceLimit != null && plan.deviceLimit > 0) {
                    "${plan.deviceLimit} thiết bị"
                } else {
                    "Không giới hạn"
                }
                tvPlanDevice.text = deviceText

                // Show lowest price
                val prices = listOfNotNull(
                    plan.monthPrice,
                    plan.quarterPrice,
                    plan.halfYearPrice,
                    plan.yearPrice
                )
                val minPrice = prices.minOrNull() ?: 0
                tvPlanPrice.text = "Từ ${Utils.formatPrice(minPrice)}"

                root.setOnClickListener { onClick(plan) }
            }
        }

        override fun getItemCount() = items.size
    }
}
