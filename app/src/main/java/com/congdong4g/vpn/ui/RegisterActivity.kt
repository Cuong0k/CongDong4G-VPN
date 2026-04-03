package com.congdong4g.vpn.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.CongDong4GApp
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityRegisterBinding
import com.congdong4g.vpn.model.RegisterRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val inviteCode = binding.etInviteCode.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Email không hợp lệ"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Vui lòng nhập mật khẩu"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.etPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.etConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                return@setOnClickListener
            }

            register(email, password, inviteCode.ifEmpty { null })
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun register(email: String, password: String, inviteCode: String?) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.register(
                    RegisterRequest(email, password, inviteCode)
                )

                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()?.data
                    val token = authData?.authData ?: authData?.token

                    if (token != null) {
                        // Save token
                        CongDong4GApp.instance.prefsManager.token = token
                        CongDong4GApp.instance.prefsManager.email = email

                        Toast.makeText(this@RegisterActivity, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()

                        // Go to main
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Lỗi đăng ký", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val message = response.body()?.message ?: "Đăng ký thất bại"
                    Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@RegisterActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
            }
        }
    }
}
