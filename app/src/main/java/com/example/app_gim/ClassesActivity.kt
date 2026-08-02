package com.example.app_gim

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.BookClassRequest
import com.example.app_gim.network.ClassSchedule
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class ClassesActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classes)

        session = SessionManager(this)
        container = findViewById(R.id.containerClasses)
        txtStatus = findViewById(R.id.txtStatus)

        loadClasses()
        findViewById<TextView>(R.id.txtToolbarTitle).text = "Lịch lớp học"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        loadBalanceInto(lifecycleScope, session, findViewById(R.id.txtBalanceCorner))
        findViewById<Button>(R.id.btnMyBookings).setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }
    }

    private fun loadClasses() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getClasses()
                if (response.isSuccessful) {
                    val classes = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (classes.isEmpty()) {
                        txtStatus.text = "Hiện chưa có lịch lớp học nào"
                    } else {
                        for (item in classes) {
                            container.addView(buildClassRow(item))
                        }
                    }
                } else {
                    txtStatus.text = "Không tải được lịch học"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildClassRow(item: ClassSchedule): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF0F0F0.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val txtName = TextView(this).apply {
            text = item.className
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val slotsLeft = item.capacity - item.bookedCount
        val txtDetail = TextView(this).apply {
            text = "Thời gian: ${item.startTime} — ${item.endTime}\n" +
                    "Phòng: ${item.room ?: "Chưa xác định"}\n" +
                    "Còn trống: $slotsLeft/${item.capacity} chỗ"
            textSize = 14f
        }

        val btnBook = Button(this).apply {
            text = if (slotsLeft > 0) "Đặt lịch" else "Đã đầy chỗ"
            isEnabled = slotsLeft > 0
            setOnClickListener { bookClass(item) }
        }

        row.addView(txtName)
        row.addView(txtDetail)
        row.addView(btnBook)
        return row
    }

    private fun bookClass(item: ClassSchedule) {
        val token = session.getToken()
        if (token == null) {
            txtStatus.text = "Vui lòng đăng nhập trước"
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.bookClass(
                    "Bearer $token",
                    BookClassRequest(scheduleId = item.scheduleId)
                )
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ClassesActivity,
                        "Đặt lịch \"${item.className}\" thành công!",
                        Toast.LENGTH_LONG
                    ).show()
                    loadClasses() // tải lại danh sách để cập nhật số chỗ còn trống
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Không thể đặt lịch"
                    txtStatus.text = errorMsg
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}