package com.example.app_gim.admin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.R
import com.example.app_gim.SessionManager
import com.example.app_gim.network.AdminTrainer
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.SaveTrainerRequest
import kotlinx.coroutines.launch

class ManageTrainersActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_trainers)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Quản lý HLV"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerItems)
        txtStatus = findViewById(R.id.txtStatus)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtSpecialty = findViewById<EditText>(R.id.edtSpecialty)
        val edtPrice = findViewById<EditText>(R.id.edtPrice)

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val name = edtName.text.toString().trim()
            val price = edtPrice.text.toString().toDoubleOrNull()
            if (name.isEmpty()) {
                txtStatus.text = "Vui lòng nhập họ tên"
                return@setOnClickListener
            }
            createTrainer(name, edtPhone.text.toString().trim(), edtSpecialty.text.toString().trim(), price)
            edtName.text.clear()
            edtPhone.text.clear()
            edtSpecialty.text.clear()
            edtPrice.text.clear()
        }

        loadTrainers()
    }

    private fun loadTrainers() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminTrainers("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    for (item in items) container.addView(buildRow(item))
                } else {
                    txtStatus.text = "Không tải được danh sách HLV"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildRow(item: AdminTrainer): LinearLayout {
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
            text = "${item.fullName}\nSĐT: ${item.phone ?: "-"}\nChuyên môn: ${item.specialty ?: "-"}\n" +
                    "Giá mỗi buổi: ${"%,.0f".format(item.pricePerSession ?: 0.0)}đ"
        }

        val btnDelete = Button(this).apply {
            text = "Xoá"
            setBackgroundColor(0xFFFF5252.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
            setOnClickListener { deleteTrainer(item) }
        }

        row.addView(txtInfo)
        row.addView(btnDelete)
        return row
    }

    private fun createTrainer(name: String, phone: String, specialty: String, price: Double?) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createTrainer(
                    "Bearer $token",
                    SaveTrainerRequest(name, phone.ifEmpty { null }, specialty.ifEmpty { null }, price)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageTrainersActivity, "Thêm HLV thành công", Toast.LENGTH_SHORT).show()
                    loadTrainers()
                } else {
                    txtStatus.text = "Không thể thêm HLV"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun deleteTrainer(item: AdminTrainer) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteTrainer("Bearer $token", item.trainerId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageTrainersActivity, "Đã xoá ${item.fullName}", Toast.LENGTH_SHORT).show()
                    loadTrainers()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Không thể xoá"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}