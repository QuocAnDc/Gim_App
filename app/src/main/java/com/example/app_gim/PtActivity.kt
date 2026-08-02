package com.example.app_gim

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
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
        findViewById<TextView>(R.id.txtToolbarTitle).text = "Huấn luyện viên"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        loadBalanceInto(lifecycleScope, session, findViewById(R.id.txtBalanceCorner))
        findViewById<Button>(R.id.btnMySessions).setOnClickListener {
            startActivity(Intent(this, MyPtSessionsActivity::class.java))
        }
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
            setBackgroundResource(R.drawable.rounded_card_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val txtName = TextView(this).apply {
            text = "${trainer.fullName}  ⭐${trainer.rating}"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val txtDetail = TextView(this).apply {
            text = "Chuyên môn: ${trainer.specialty ?: "Chưa cập nhật"}\n" +
                    "Giá mỗi buổi: ${"%,.0f".format(trainer.pricePerSession)}đ"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
        }

        val btnInfo = Button(this).apply {
            text = "Xem thông tin"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showTrainerInfo(trainer) }
        }

        val btnBook = Button(this).apply {
            text = "Đặt lịch"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
            setOnClickListener { pickDateTime(trainer) }
        }

        btnRow.addView(btnInfo)
        btnRow.addView(btnBook)

        row.addView(txtName)
        row.addView(txtDetail)
        row.addView(btnRow)
        return row
    }

    private fun showTrainerInfo(trainer: Trainer) {
        val message = buildString {
            append("Họ tên: ${trainer.fullName}\n\n")
            append("Đánh giá: ⭐ ${trainer.rating}/5\n")
            append("Chuyên môn: ${trainer.specialty ?: "Chưa cập nhật"}\n")
            append("Số điện thoại: ${trainer.phone ?: "Chưa cập nhật"}\n")
            append("Giá mỗi buổi: ${"%,.0f".format(trainer.pricePerSession)}đ")
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Thông tin huấn luyện viên")
            .setMessage(message)
            .setPositiveButton("Đặt lịch ngay") { _, _ -> pickDateTime(trainer) }
            .setNegativeButton("Đóng", null)
            .show()
    }
    // Mở DatePicker -> sau đó mở tiếp TimePicker -> ghép lại thành chuỗi datetime chuẩn
    // Mở DatePicker -> sau đó mở tiếp TimePicker -> ghép lại thành chuỗi datetime chuẩn
    private fun pickDateTime(trainer: Trainer) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, R.style.PickerDialogTheme, { _, year, month, day ->
            TimePickerDialog(this, R.style.PickerDialogTheme, { _, hour, minute ->
                val scheduledTime = String.format(
                    "%04d-%02d-%02d %02d:%02d:00",
                    year, month + 1, day, hour, minute
                )
                android.app.AlertDialog.Builder(this)
                    .setTitle("Xác nhận đặt lịch")
                    .setMessage("Đặt lịch với ${trainer.fullName} vào $scheduledTime\nGiá: ${"%,.0f".format(trainer.pricePerSession)}đ (trừ từ ví)")
                    .setPositiveButton("Đồng ý") { _, _ -> bookPt(trainer, scheduledTime) }
                    .setNegativeButton("Huỷ", null)
                    .show()
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
                    loadBalanceInto(lifecycleScope, session, findViewById(R.id.txtBalanceCorner))
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