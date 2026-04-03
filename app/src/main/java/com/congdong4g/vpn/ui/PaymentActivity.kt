package com.congdong4g.vpn.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.CongDong4GApp
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityPaymentBinding
import com.congdong4g.vpn.model.CreateOrderRequest
import com.congdong4g.vpn.utils.Utils
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    
    private var planId: Int = 0
    private var planName: String = ""
    private var monthPrice: Int = 0
    private var quarterPrice: Int = 0
    private var halfYearPrice: Int = 0
    private var yearPrice: Int = 0
    private var selectedPeriod: String = "month_price"
    private var selectedPrice: Int = 0

    // Thông tin chuyển khoản - CẬP NHẬT THEO CỦA BẠN
    private val bankName = "Vietcombank"
    private val bankAccount = "1234567890"
    private val accountName = "NGUYEN VAN A"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()
        setupViews()
    }

    private fun getIntentData() {
        planId = intent.getIntExtra("plan_id", 0)
        planName = intent.getStringExtra("plan_name") ?: ""
        monthPrice = intent.getIntExtra("plan_month_price", 0)
        quarterPrice = intent.getIntExtra("plan_quarter_price", 0)
        halfYearPrice = intent.getIntExtra("plan_half_year_price", 0)
        yearPrice = intent.getIntExtra("plan_year_price", 0)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.tvPlanName.text = planName

        // Setup period options
        setupPeriodOptions()

        // Bank info
        binding.tvBankName.text = bankName
        binding.tvBankAccount.text = bankAccount
        binding.tvAccountName.text = accountName

        // Copy buttons
        binding.btnCopyAccount.setOnClickListener {
            copyToClipboard("Số tài khoản", bankAccount)
        }

        binding.btnCopyContent.setOnClickListener {
            val content = binding.tvTransferContent.text.toString()
            copyToClipboard("Nội dung CK", content)
        }

        // Submit button
        binding.btnSubmitOrder.setOnClickListener {
            createOrder()
        }
    }

    private fun setupPeriodOptions() {
        binding.radioGroup.removeAllViews()

        val periods = mutableListOf<Pair<String, Int>>()
        if (monthPrice > 0) periods.add("month_price" to monthPrice)
        if (quarterPrice > 0) periods.add("quarter_price" to quarterPrice)
        if (halfYearPrice > 0) periods.add("half_year_price" to halfYearPrice)
        if (yearPrice > 0) periods.add("year_price" to yearPrice)

        periods.forEachIndexed { index, (period, price) ->
            val radioButton = RadioButton(this).apply {
                id = View.generateViewId()
                text = when (period) {
                    "month_price" -> "1 Tháng - ${Utils.formatPrice(price)}"
                    "quarter_price" -> "3 Tháng - ${Utils.formatPrice(price)}"
                    "half_year_price" -> "6 Tháng - ${Utils.formatPrice(price)}"
                    "year_price" -> "12 Tháng - ${Utils.formatPrice(price)}"
                    else -> period
                }
                textSize = 16f
                setPadding(0, 16, 0, 16)
                
                setOnClickListener {
                    selectedPeriod = period
                    selectedPrice = price
                    updatePaymentInfo()
                }
            }
            binding.radioGroup.addView(radioButton)

            // Select first option by default
            if (index == 0) {
                radioButton.isChecked = true
                selectedPeriod = period
                selectedPrice = price
                updatePaymentInfo()
            }
        }
    }

    private fun updatePaymentInfo() {
        binding.tvAmount.text = Utils.formatPrice(selectedPrice)
        
        // Generate transfer content
        val email = CongDong4GApp.instance.prefsManager.email ?: "user"
        val content = "${email.substringBefore("@")} $planName"
        binding.tvTransferContent.text = content
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Đã sao chép $label", Toast.LENGTH_SHORT).show()
    }

    private fun createOrder() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmitOrder.isEnabled = false

        lifecycleScope.launch {
            try {
                val token = CongDong4GApp.instance.prefsManager.token ?: return@launch
                val response = ApiClient.apiService.createOrder(
                    "$token",
                    CreateOrderRequest(planId, selectedPeriod)
                )

                if (response.isSuccessful && response.body()?.data != null) {
                    val tradeNo = response.body()?.data
                    showOrderCreatedDialog(tradeNo ?: "")
                } else {
                    val message = response.body()?.message ?: "Tạo đơn hàng thất bại"
                    Toast.makeText(this@PaymentActivity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PaymentActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitOrder.isEnabled = true
            }
        }
    }

    private fun showOrderCreatedDialog(tradeNo: String) {
        AlertDialog.Builder(this)
            .setTitle("Đơn hàng đã tạo")
            .setMessage("""
                Mã đơn hàng: $tradeNo
                
                Vui lòng chuyển khoản với nội dung:
                ${binding.tvTransferContent.text}
                
                Đơn hàng sẽ được kích hoạt sau khi thanh toán được xác nhận.
            """.trimIndent())
            .setPositiveButton("Đã hiểu") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
