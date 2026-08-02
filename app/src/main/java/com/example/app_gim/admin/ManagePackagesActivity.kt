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
import com.example.app_gim.network.AdminPackage
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.SavePackageRequest
import kotlinx.coroutines.launch

class ManagePackagesActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_packages)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Quản lý Gói tập"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerItems)
        txtStatus = findViewById(R.id.txtStatus)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtDuration = findViewById<EditText>(R.id.edtDuration)
        val edtPrice = findViewById<EditText>(R.id.edtPrice)

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val name = edtName.text.toString().trim()
            val duration = edtDuration.text.toString().toIntOrNull()
            val price = edtPrice.text.toString().toDoubleOrNull()

            if (name.isEmpty() || duration == null || price == null) {
                txtStatus.text = "Vui lòng nhập đủ và đúng định dạng"
                return@setOnClickListener
            }

            createPackage(name, duration, price)
            edtName.text.clear()
            edtDuration.text.clear()
            edtPrice.text.clear()
        }

        loadPackages()
    }

    private fun loadPackages() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminPackages("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    for (item in items) {
                        container.addView(buildRow(item))
                    }
                } else {
                    txtStatus.text = "Không tải được danh sách gói"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildRow(item: AdminPackage): LinearLayout {
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
            text = "${item.name}\n${item.durationDays} ngày — ${"%,.0f".format(item.price)}đ"
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

        val btnDelete = Button(this).apply {
            text = "Xoá"
            setBackgroundColor(0xFFFF5252.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { deletePackage(item) }
        }

        btnRow.addView(btnDelete)
        row.addView(txtInfo)
        row.addView(btnRow)
        return row
    }

    private fun createPackage(name: String, duration: Int, price: Double) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createPackage(
                    "Bearer $token", SavePackageRequest(name, duration, price)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ManagePackagesActivity, "Thêm gói thành công", Toast.LENGTH_SHORT).show()
                    loadPackages()
                } else {
                    txtStatus.text = "Không thể thêm gói"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun deletePackage(item: AdminPackage) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deletePackage("Bearer $token", item.packageId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ManagePackagesActivity, "Đã xoá ${item.name}", Toast.LENGTH_SHORT).show()
                    loadPackages()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Không thể xoá"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}