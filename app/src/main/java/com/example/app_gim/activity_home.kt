package com.example.app_gim

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val session = SessionManager(this)
        val txtWelcome = findViewById<TextView>(R.id.txtWelcome)
        txtWelcome.text = "Chào mừng, ${session.getFullName()}!"

        loadMemberInfo(session, txtWelcome)
        val btnPackages = findViewById<Button>(R.id.btnPackages)
        btnPackages.setOnClickListener {
            startActivity(Intent(this, PackagesActivity::class.java))
        }
        val btnCheckin = findViewById<Button>(R.id.btnCheckin)
        btnCheckin.setOnClickListener {
            doCheckin(session)
        }

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        val btnClasses = findViewById<Button>(R.id.btnClasses)
        btnClasses.setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java))
        }
        val btnPt = findViewById<Button>(R.id.btnPt)
        btnPt.setOnClickListener {
            startActivity(Intent(this, PtActivity::class.java))
        }
        val btnLockers = findViewById<Button>(R.id.btnLockers)
        btnLockers.setOnClickListener {
            startActivity(Intent(this, LockersActivity::class.java))
        }
        val btnWallet = findViewById<Button>(R.id.btnWallet)
        btnWallet.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }
    }

    private fun loadMemberInfo(session: SessionManager, txtWelcome: TextView) {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyInfo("Bearer $token")
                if (response.isSuccessful) {
                    val info = response.body()
                    txtWelcome.text = buildString {
                        append("Chào mừng, ${info?.fullName}!\n\n")
                        if (info?.cardCode != null) {
                            append("Mã thẻ: ${info.cardCode}\n")
                            append("Gói: ${info.packageName}\n")
                            append("Trạng thái: ${info.cardStatus}\n")
                            append("Hết hạn: ${info.expiryDate}")
                        } else {
                            append("Bạn chưa có thẻ tập nào.\nHãy mua gói tập để bắt đầu!")
                        }
                    }
                } else {
                    txtWelcome.text = "Không tải được thông tin thẻ"
                }
            } catch (e: Exception) {
                txtWelcome.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
    private fun doCheckin(session: SessionManager) {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.checkin("Bearer $token")
                if (response.isSuccessful) {
                    val result = response.body()
                    Toast.makeText(
                        this@HomeActivity,
                        result?.message ?: "Thành công",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Không thể check-in"
                    Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}