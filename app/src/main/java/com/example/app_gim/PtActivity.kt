package com.example.app_gim

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.BookPtRequest
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.Trainer
import kotlinx.coroutines.launch
import java.util.Calendar

class PtActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pt)

        session = SessionManager(this)
        container = findViewById(R.id.containerTrainers)
        txtStatus = findViewById(R.id.txtStatus)

        loadTrainers()
    }

    private fun loadTrainers() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getTrainers()
                if (response.isSuccessful) {
                    val trainers = response.body() ?: emptyList()
                    container.removeAllViews()
                    for (t in trainers) {
                        container.addView(buildTrainerRow(t))
                    }
                } else {
                    txtStatus.text = "Không tải được danh sách HLV"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildTrainerRow(trainer: Trainer): LinearLayout {
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
            text = "${trainer.fullName}  ⭐${trainer.rating}"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val txtDetail = TextView(this).apply {
            text = "Chuyên môn: ${trainer.specialty ?: "Chưa cập nhật"}\nSĐT: ${trainer.phone ?: "-"}"
            textSize = 14f
        }

        val btnBook = Button(this).apply {
            text = "Đặt lịch tập"
            setOnClickListener { pickDateTime(trainer) }
        }

        row.addView(txtName)
        row.addView(txtDetail)
        row.addView(btnBook)
        return row
    }

    // Mở DatePicker -> sau đó mở tiếp TimePicker -> ghép lại thành chuỗi datetime chuẩn
    private fun pickDateTime(trainer: Trainer) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                // Định dạng: yyyy-MM-dd HH:mm:ss (khớp với kiểu DATETIME của MySQL)
                val scheduledTime = String.format(
                    "%04d-%02d-%02d %02d:%02d:00",
                    year, month + 1, day, hour, minute
                )
                bookPt(trainer, scheduledTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun bookPt(trainer: Trainer, scheduledTime: String) {
        val token = session.getToken()
        if (token == null) {
            txtStatus.text = "Vui lòng đăng nhập trước"
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.bookPt(
                    "Bearer $token",
                    BookPtRequest(trainerId = trainer.trainerId, scheduledTime = scheduledTime)
                )
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@PtActivity,
                        "Đặt lịch với ${trainer.fullName} lúc $scheduledTime thành công!",
                        Toast.LENGTH_LONG
                    ).show()
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