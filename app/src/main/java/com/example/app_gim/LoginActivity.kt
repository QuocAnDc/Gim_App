package com.example.app_gim

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.LoginRequest
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        btnLogin.setOnClickListener {
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                txtStatus.text = "Vui lòng nhập đủ SĐT và Mật khẩu"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.login(
                        LoginRequest(phone = phone, password = password)
                    )

                    if (response.isSuccessful) {
                        val loginData = response.body()
                        if (loginData != null) {
                            SessionManager(this@LoginActivity).saveSession(
                                token = loginData.token,
                                userId = loginData.user.userId,
                                fullName = loginData.user.fullName
                            )
                        }
                        txtStatus.text = "Đăng nhập thành công: ${loginData?.user?.fullName}"
                        Toast.makeText(
                            this@LoginActivity,
                            "Chào ${loginData?.user?.fullName}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    } else {
                        txtStatus.text = "Sai số điện thoại hoặc mật khẩu"
                    }
                } catch (e: Exception) {
                    txtStatus.text = "Không kết nối được server: ${e.message}"
                }
            }

        }
        val txtGoRegister = findViewById<TextView>(R.id.txtGoRegister)
        txtGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

}