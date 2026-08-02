package com.example.app_gim

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.MyBooking
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Lịch của tôi"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerBookings)
        txtStatus = findViewById(R.id.txtStatus)

        loadBookings()
    }

    private fun loadBookings() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyBookings("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        txtStatus.text = "Bạn chưa đặt lịch lớp học nào"
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

    private fun buildRow(item: MyBooking): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF1E2733.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val txtInfo = TextView(this).apply {
            text = "${item.className}\nThời gian: ${item.startTime} — ${item.endTime}\nPhòng: ${item.room ?: "-"}"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
        }

        val btnCancel = Button(this).apply {
            text = "Hủy lịch"
            setBackgroundColor(0xFFFF5252.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
            setOnClickListener { confirmCancel(item) }
        }

        row.addView(txtInfo)
        row.addView(btnCancel)
        return row
    }

    private fun confirmCancel(item: MyBooking) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận hủy lịch")
            .setMessage("Bạn có chắc muốn hủy lịch \"${item.className}\" không?")
            .setPositiveButton("Đồng ý") { _, _ -> cancelBooking(item) }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun cancelBooking(item: MyBooking) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.cancelBooking("Bearer $token", item.bookingId)
                if (response.isSuccessful) {
                    Toast.makeText(this@MyBookingsActivity, "Đã hủy lịch", Toast.LENGTH_SHORT).show()
                    loadBookings()
                } else {
                    txtStatus.text = "Không thể hủy lịch"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}