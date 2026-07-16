package com.example.app_gim

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        if (session.isLoggedIn()) {
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // Chưa đăng nhập -> bắt đăng nhập
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish() // đóng MainActivity lại, không cho bấm Back quay về màn hình trống này
    }
}