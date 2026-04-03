package com.congdong4g.vpn.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityPaymentBinding
import com.congdong4g.vpn.model.CreateOrderRequest
import com.congdong4g.vpn.utils.PrefsManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var prefs: PrefsManager

    private val bankCode = "MB"
    private val bankAccount = "555585678"
    private val accountName = "DO VAN CUONG"

    private var planId: Int = 0
    private var planName: String = ""
    private var planPrice: Long = 0
    private var selectedPeriod: String = "month_price"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        planId = intent.getIntExtra("plan_id", 0)
        planName = intent.getStringExtra("plan_name") ?: ""
        planPrice = intent.getLongExtra("plan_price", 0)
        selectedPeriod = intent.getStringExtra("period") ?: "month_price"

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvPlanName.text = planName
        binding.tvPrice.text = formatPrice(planPrice)
        binding.tvPeriod.text = getPeriodText(selectedPeriod)
        binding.tvBankName.text = bankCode
        binding.tvAccountNumber.text = bankAccount
        binding.tvAccountName.text = accountName
    }

    private fun setupClickListeners() {
        binding.btnCopyAccount.setOnClickListener {
            copyToClipboard(bankAccount, "Số tài khoản")
        }
        binding.btnCopyAmount.setOnClickListener {
            copyToClipboard(planPrice.toString(), "Số tiền")
        }
        binding.btnSubmitOrder.setOnClickListener {
            createOrder()
        }
    }

    private fun createOrder() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmitOrder.isEnabled = false

        lifecycleScope.launch {
            try {
                val authData = prefs.token ?: ""
                val response = ApiClient.apiService.createOrder(
                    authData,
                    CreateOrderRequest(planId, selectedPeriod)
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    val tradeNo = response.body()?.data ?: ""
                    showQRPayment(tradeNo)
                } else {
                    val message = response.body()?.message ?: "Tạo đơn hàng thất bại"
                    Toast.makeText(this@PaymentActivity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitOrder.isEnabled = true
            }
        }
    }

    private fun showQRPayment(tradeNo: String) {
        binding.layoutOrderInfo.visibility = View.VISIBLE
        binding.tvTradeNo.text = tradeNo
        binding.btnCopyTradeNo.setOnClickListener {
            copyToClipboard(tradeNo, "Mã đơn hàng")
        }
        val qrUrl = "https://img.vietqr.io/image/$bankCode-$bankAccount-compact2.png?amount=$planPrice&addInfo=$tradeNo&accountName=$accountName"
        binding.ivQrCode.visibility = View.VISIBLE
        Glide.with(this).load(qrUrl).into(binding.ivQrCode)
        binding.tvQrNote.visibility = View.VISIBLE
        binding.tvQrNote.text = "Quét mã QR để thanh toán\nNội dung: $tradeNo"
    }

    private fun copyToClipboard(text: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, "Đã sao chép $label", Toast.LENGTH_SHORT).show()
    }

    private fun formatPrice(price: Long): String {
        return "${NumberFormat.getInstance(Locale("vi", "VN")).format(price)} đ"
    }

    private fun getPeriodText(period: String) = when (period) {
        "month_price" -> "1 tháng"
        "quarter_price" -> "3 tháng"
        "half_year_price" -> "6 tháng"
        "year_price" -> "12 tháng"
        else -> "1 tháng"
    }
}
