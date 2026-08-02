package com.example.app_gim.admin

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.R
import com.example.app_gim.SessionManager
import com.example.app_gim.network.AdminCheckinLog
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class AdminCheckinLogsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_checkin_logs)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Check-in / Check-out"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerItems)
        txtStatus = findViewById(R.id.txtStatus)

        loadLogs()
    }

    private fun loadLogs() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminCheckinLogs("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        txtStatus.text = "Chưa có dữ liệu check-in nào"
                    } else {
                        txtStatus.text = ""
                        for (item in items) container.addView(buildRow(item))
                    }
                } else {
                    txtStatus.text = "Không tải được danh sách"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildRow(item: AdminCheckinLog): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundResource(R.drawable.rounded_card_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }

        val txtInfo = TextView(this).apply {
            text = "${item.memberName}\nCheck-in: ${item.checkinTime}\n" +
                    (item.checkoutTime?.let { "Check-out: $it" } ?: "Đang trong phòng gym...")
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        }
        row.addView(txtInfo)
        return row
    }
}