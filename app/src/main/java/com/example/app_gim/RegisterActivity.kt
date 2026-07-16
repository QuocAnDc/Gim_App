package com.example.app_gim

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.RegisterRequest
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtFullName = findViewById<EditText>(R.id.edtFullName)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        btnRegister.setOnClickListener {
            val fullName = edtFullName.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (fullName.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                txtStatus.text = "Vui lòng nhập đủ Họ tên, SĐT và Mật khẩu"
                return@setOnClickListener
            }

            // Chạy trong coroutine vì gọi mạng, tránh đứng hình giao diện
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.register(
                        RegisterRequest(
                            fullName = fullName,
                            phone = phone,
                            email = email.ifEmpty { null },
                            password = password
                        )
                    )

                    if (response.isSuccessful) {
                        txtStatus.text = "Đăng ký thành công!"
                        Toast.makeText(this@RegisterActivity, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Server trả lỗi (vd: trùng SĐT) -> đọc nội dung lỗi
                        val errorBody = response.errorBody()?.string()
                        txtStatus.text = "Lỗi: $errorBody"
                    }
                } catch (e: Exception) {
                    // Lỗi mạng (không kết nối được server...)
                    txtStatus.text = "Không kết nối được server: ${e.message}"
                }
            }
        }
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)
        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}