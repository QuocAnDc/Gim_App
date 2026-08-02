package com.example.app_gim

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.CheckinHistoryItem
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class CheckinHistoryActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkin_history)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Lịch sử Check-in"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtBalanceCorner).visibility = android.view.View.GONE

        session = SessionManager(this)
        container = findViewById(R.id.containerHistory)
        txtStatus = findViewById(R.id.txtStatus)

        loadHistory()
    }

    private fun loadHistory() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCheckinHistory("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        txtStatus.text = "Chưa có lịch sử check-in nào"
                    } else {
                        txtStatus.text = ""
                        for (item in items) container.addView(buildRow(item))
                    }
                } else {
                    txtStatus.text = "Không tải được lịch sử"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildRow(item: CheckinHistoryItem): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundResource(R.drawable.rounded_card_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }

        val txtCheckin = TextView(this).apply {
            text = "Check-in: ${item.checkinTime}"
            setTextColor(0xFF00E676.toInt())
            textSize = 15f
        }

        val txtCheckout = TextView(this).apply {
            text = if (item.checkoutTime != null) "Check-out: ${item.checkoutTime}" else "Đang trong phòng gym..."
            setTextColor(if (item.checkoutTime != null) 0xFFFF5252.toInt() else 0xFFFFAB00.toInt())
            textSize = 15f
        }

        row.addView(txtCheckin)
        row.addView(txtCheckout)
        return row
    }
}