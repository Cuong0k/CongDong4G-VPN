package com.congdong4g.vpn.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.CongDong4GApp
import com.congdong4g.vpn.api.ApiClient
import com.congdong4g.vpn.databinding.ActivityLoginBinding
import com.congdong4g.vpn.model.LoginRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Vui lòng nhập mật khẩu"
                return@setOnClickListener
            }

            login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            // Handle forgot password
            Toast.makeText(this, "Vui lòng liên hệ Admin để đặt lại mật khẩu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun login(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.login(LoginRequest(email, password))
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val authData = response.body()?.data
                    val token = authData?.authData ?: authData?.token
                    
                    if (token != null) {
                        // Save token
                        CongDong4GApp.instance.prefsManager.token = token
                        CongDong4GApp.instance.prefsManager.email = email
                        
                        Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        
                        // Go to main
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Lỗi đăng nhập", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val message = response.body()?.message ?: "Email hoặc mật khẩu không đúng"
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }
}
