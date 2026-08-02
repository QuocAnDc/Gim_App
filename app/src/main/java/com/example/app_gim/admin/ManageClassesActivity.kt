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
import com.example.app_gim.network.AdminClass
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.SaveClassRequest
import kotlinx.coroutines.launch

class ManageClassesActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_classes)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Quản lý Lớp học"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerItems)
        txtStatus = findViewById(R.id.txtStatus)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtDescription = findViewById<EditText>(R.id.edtDescription)
        val edtCapacity = findViewById<EditText>(R.id.edtCapacity)

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val name = edtName.text.toString().trim()
            val description = edtDescription.text.toString().trim()
            val capacity = edtCapacity.text.toString().toIntOrNull()

            if (name.isEmpty()) {
                txtStatus.text = "Vui lòng nhập tên lớp"
                return@setOnClickListener
            }

            createClass(name, description.ifEmpty { null }, capacity ?: 20)
            edtName.text.clear()
            edtDescription.text.clear()
            edtCapacity.text.clear()
        }

        loadClasses()
    }

    private fun loadClasses() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminClasses("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    for (item in items) container.addView(buildRow(item))
                } else {
                    txtStatus.text = "Không tải được danh sách lớp học"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildRow(item: AdminClass): LinearLayout {
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
            text = "${item.name}\n${item.description ?: "Không có mô tả"}\nSức chứa: ${item.capacity}"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
        }

        val btnSchedule = Button(this).apply {
            text = "Quản lý lịch"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val intent = android.content.Intent(this@ManageClassesActivity, ManageSchedulesActivity::class.java)
                intent.putExtra("classId", item.classId)
                intent.putExtra("className", item.name)
                startActivity(intent)
            }
        }

        val btnDelete = Button(this).apply {
            text = "Xoá"
            setBackgroundColor(0xFFFF5252.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
            setOnClickListener { deleteClass(item) }
        }

        btnRow.addView(btnSchedule)
        btnRow.addView(btnDelete)

        row.addView(txtInfo)
        row.addView(btnRow)
        return row
    }

    private fun createClass(name: String, description: String?, capacity: Int) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createClass(
                    "Bearer $token", SaveClassRequest(name, description, capacity)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageClassesActivity, "Thêm lớp học thành công", Toast.LENGTH_SHORT).show()
                    loadClasses()
                } else {
                    txtStatus.text = "Không thể thêm lớp học"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun deleteClass(item: AdminClass) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteClass("Bearer $token", item.classId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageClassesActivity, "Đã xoá ${item.name}", Toast.LENGTH_SHORT).show()
                    loadClasses()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Không thể xoá"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}