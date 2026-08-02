package com.example.app_gim.admin

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.R
import com.example.app_gim.SessionManager
import com.example.app_gim.network.AdminPtSession
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.UpdatePtStatusRequest
import kotlinx.coroutines.launch

class ManagePtSessionsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_pt_sessions)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Duyệt lịch PT"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerItems)
        txtStatus = findViewById(R.id.txtStatus)

        loadSessions()
    }

    private fun loadSessions() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminPtSessions("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        txtStatus.text = "Chưa có lịch PT nào"
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

    private fun buildRow(item: AdminPtSession): LinearLayout {
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
            "pending" -> "Đang chờ duyệt"
            "confirmed" -> "Đã duyệt"
            "rejected" -> "Đã từ chối"
            "completed" -> "Đã hoàn thành"
            "cancelled" -> "Hội viên đã hủy"
            else -> item.status
        }

        val txtInfo = TextView(this).apply {
            text = "Hội viên: ${item.memberName}\nHLV: ${item.trainerName}\n" +
                    "Thời gian: ${item.scheduledTime}\nTrạng thái: $statusText"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
        }
        row.addView(txtInfo)

        if (item.status == "pending") {
            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
            }

            val btnApprove = Button(this).apply {
                text = "Duyệt"
                setBackgroundColor(0xFF00E676.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { updateStatus(item.sessionId, "confirmed") }
            }

            val btnReject = Button(this).apply {
                text = "Từ chối"
                setBackgroundColor(0xFFFF5252.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 12
                }
                setOnClickListener { updateStatus(item.sessionId, "rejected") }
            }

            btnRow.addView(btnApprove)
            btnRow.addView(btnReject)
            row.addView(btnRow)
        } else if (item.status == "confirmed") {
            val btnComplete = Button(this).apply {
                text = "Đánh dấu hoàn thành"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
                setOnClickListener { updateStatus(item.sessionId, "completed") }
            }
            row.addView(btnComplete)
        }

        return row
    }

    private fun updateStatus(sessionId: Int, status: String) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updatePtSessionStatus(
                    "Bearer $token", sessionId, UpdatePtStatusRequest(status)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ManagePtSessionsActivity, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                    loadSessions()
                } else {
                    txtStatus.text = "Không thể cập nhật"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}