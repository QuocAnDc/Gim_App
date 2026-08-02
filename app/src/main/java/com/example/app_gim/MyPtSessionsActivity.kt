package com.example.app_gim

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.PtSession
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class MyPtSessionsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_pt_sessions)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Lịch PT đã đặt"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtBalanceCorner).visibility = android.view.View.GONE

        session = SessionManager(this)
        container = findViewById(R.id.containerSessions)
        txtStatus = findViewById(R.id.txtStatus)

        loadSessions()
    }

    private fun loadSessions() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMySessions("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        txtStatus.text = "Bạn chưa đặt lịch PT nào"
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

    private fun buildRow(item: PtSession): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundResource(R.drawable.rounded_card_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val statusText = when (item.status) {
            "pending" -> "Đang chờ xác nhận"
            "confirmed" -> "Đã xác nhận"
            "rejected" -> "Đã từ chối"
            "completed" -> "Đã hoàn thành"
            "cancelled" -> "Đã hủy"
            else -> item.status
        }

        val txtInfo = TextView(this).apply {
            text = "HLV: ${item.trainerName}\n" +
                    "Thời gian: ${item.scheduledTime}\n" +
                    "Trạng thái: $statusText" +
                    (if (!item.note.isNullOrEmpty()) "\nGhi chú: ${item.note}" else "")
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
        }

        row.addView(txtInfo)

        // Chỉ hiện nút Hủy nếu lịch đang chờ hoặc đã xác nhận (chưa hoàn thành/đã hủy)
        if (item.status == "pending" || item.status == "confirmed") {
            val btnCancel = Button(this).apply {
                text = "Hủy lịch"
                setBackgroundColor(0xFFFF5252.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { confirmCancel(item) }
            }
            row.addView(btnCancel)
        }

        return row
    }

    private fun confirmCancel(item: PtSession) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận hủy lịch")
            .setMessage("Bạn có chắc muốn hủy lịch PT với ${item.trainerName} không?")
            .setPositiveButton("Đồng ý") { _, _ -> cancelSession(item) }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun cancelSession(item: PtSession) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.cancelPtSession("Bearer $token", item.sessionId)
                if (response.isSuccessful) {
                    Toast.makeText(this@MyPtSessionsActivity, "Đã hủy lịch", Toast.LENGTH_SHORT).show()
                    loadSessions()
                } else {
                    txtStatus.text = "Không thể hủy lịch"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}