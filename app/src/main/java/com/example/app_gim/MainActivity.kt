package com.example.app_gim

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.app_gim.admin.AdminDashboardActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        val destination = when {
            !session.isLoggedIn() -> LoginActivity::class.java
            session.isAdmin() -> AdminDashboardActivity::class.java
            else -> HomeActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}