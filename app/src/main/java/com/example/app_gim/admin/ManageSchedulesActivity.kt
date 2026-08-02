package com.example.app_gim.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.app_gim.network.AdminSchedule
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.SaveScheduleRequest
import kotlinx.coroutines.launch
import java.util.Calendar

class ManageSchedulesActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView
    private var classId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_schedules)

        classId = intent.getIntExtra("classId", -1)
        val className = intent.getStringExtra("className") ?: "Lớp học"

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Lịch: $className"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerItems)
        txtStatus = findViewById(R.id.txtStatus)

        findViewById<Button>(R.id.btnAddSchedule).setOnClickListener { pickDateTimeAndAdd() }

        loadSchedules()
    }

    private fun loadSchedules() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getClassSchedules("Bearer $token", classId)
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        txtStatus.text = "Lớp này chưa có buổi học nào"
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

    private fun buildRow(item: AdminSchedule): LinearLayout {
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
            text = "${item.startTime} — ${item.endTime}\nPhòng: ${item.room ?: "-"}"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
        }

        val btnDelete = Button(this).apply {
            text = "Xoá"
            setBackgroundColor(0xFFFF5252.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
            setOnClickListener { deleteSchedule(item) }
        }

        row.addView(txtInfo)
        row.addView(btnDelete)
        return row
    }

    private fun pickDateTimeAndAdd() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, R.style.PickerDialogTheme, { _, year, month, day ->
            TimePickerDialog(this, R.style.PickerDialogTheme, { _, hour, minute ->
                val startTime = String.format("%04d-%02d-%02d %02d:%02d:00", year, month + 1, day, hour, minute)
                // Mặc định buổi học kéo dài 1 tiếng
                val endCalendar = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute)
                    add(Calendar.HOUR_OF_DAY, 1)
                }
                val endTime = String.format(
                    "%04d-%02d-%02d %02d:%02d:00",
                    endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH) + 1,
                    endCalendar.get(Calendar.DAY_OF_MONTH), endCalendar.get(Calendar.HOUR_OF_DAY),
                    endCalendar.get(Calendar.MINUTE)
                )
                addSchedule(startTime, endTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun addSchedule(startTime: String, endTime: String) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createSchedule(
                    "Bearer $token", classId, SaveScheduleRequest(startTime, endTime, "Phong A")
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageSchedulesActivity, "Thêm buổi học thành công", Toast.LENGTH_SHORT).show()
                    loadSchedules()
                } else {
                    txtStatus.text = "Không thể thêm buổi học"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun deleteSchedule(item: AdminSchedule) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteSchedule("Bearer $token", item.scheduleId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageSchedulesActivity, "Đã xoá buổi học", Toast.LENGTH_SHORT).show()
                    loadSchedules()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Không thể xoá"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}