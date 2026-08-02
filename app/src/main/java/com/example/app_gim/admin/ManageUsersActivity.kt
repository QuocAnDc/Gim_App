package com.example.app_gim

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.AdminUser
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.UpdateStatusRequest
import kotlinx.coroutines.launch

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Quản lý Người dùng"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerUsers)
        txtStatus = findViewById(R.id.txtStatus)

        loadUsers()
    }

    private fun loadUsers() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminUsers("Bearer $token")
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    container.removeAllViews()
                    for (u in users) {
                        container.addView(buildUserRow(u))
                    }
                } else {
                    txtStatus.text = "Không tải được danh sách người dùng"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildUserRow(user: AdminUser): LinearLayout {
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
            text = "${user.fullName} (${user.roleName})\nSĐT: ${user.phone}\nTrạng thái: ${user.status}"
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

        val isActive = user.status == "active"
        val btnToggle = Button(this).apply {
            text = if (isActive) "Khoá" else "Mở khoá"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { toggleStatus(user, isActive) }
        }

        val btnDelete = Button(this).apply {
            text = "Xoá"
            setBackgroundColor(0xFFFF5252.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
            setOnClickListener { deleteUser(user) }
        }

        btnRow.addView(btnToggle)
        btnRow.addView(btnDelete)

        row.addView(txtInfo)
        row.addView(btnRow)
        return row
    }

    private fun toggleStatus(user: AdminUser, isCurrentlyActive: Boolean) {
        val token = session.getToken() ?: return
        val newStatus = if (isCurrentlyActive) "locked" else "active"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateUserStatus(
                    "Bearer $token", user.userId, UpdateStatusRequest(newStatus)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageUsersActivity, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    txtStatus.text = "Không thể cập nhật trạng thái"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun deleteUser(user: AdminUser) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteUser("Bearer $token", user.userId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageUsersActivity, "Đã xoá ${user.fullName}", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Không thể xoá"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}