package com.congdong4g.vpn.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityForgotPasswordBinding
import com.congdong4g.vpn.model.ForgotPasswordRequest
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            when {
                email.isEmpty() -> binding.etEmail.error = "Vui lòng nhập email"
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.etEmail.error = "Email không hợp lệ"
                else -> requestReset(email)
            }
        }
    }

    private fun requestReset(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnReset.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful) {
                    binding.layoutSuccess.visibility = View.VISIBLE
                    binding.layoutForm.visibility = View.GONE
                    Toast.makeText(this@ForgotPasswordActivity, "Đã gửi yêu cầu!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { Toast.makeText(this@ForgotPasswordActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally { binding.progressBar.visibility = View.GONE; binding.btnReset.isEnabled = true }
        }
    }
}
