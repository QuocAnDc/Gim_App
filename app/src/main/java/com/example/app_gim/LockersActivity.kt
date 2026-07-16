package com.example.app_gim

import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.Locker
import com.example.app_gim.network.LockerActionRequest
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class LockersActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var grid: GridLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lockers)

        session = SessionManager(this)
        grid = findViewById(R.id.gridLockers)
        txtStatus = findViewById(R.id.txtStatus)

        loadLockers()
    }

    private fun loadLockers() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getLockers()
                if (response.isSuccessful) {
                    val lockers = response.body() ?: emptyList()
                    grid.removeAllViews()
                    for (locker in lockers) {
                        grid.addView(buildLockerButton(locker))
                    }
                } else {
                    txtStatus.text = "Không tải được danh sách tủ"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildLockerButton(locker: Locker): Button {
        val isAvailable = locker.status == "available"

        return Button(this).apply {
            text = locker.code
            setBackgroundColor(if (isAvailable) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            setTextColor(0xFFFFFFFF.toInt())

            val params = GridLayout.LayoutParams().apply {
                width = 180
                height = 180
                setMargins(8, 8, 8, 8)
            }
            layoutParams = params

            setOnClickListener {
                if (isAvailable) rentLocker(locker) else returnLocker(locker)
            }
        }
    }

    private fun rentLocker(locker: Locker) {
        val token = session.getToken() ?: run {
            txtStatus.text = "Vui lòng đăng nhập trước"
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.rentLocker(
                    "Bearer $token", LockerActionRequest(lockerId = locker.lockerId)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@LockersActivity, "Đã thuê tủ ${locker.code}", Toast.LENGTH_SHORT).show()
                    loadLockers()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Không thể thuê tủ"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun returnLocker(locker: Locker) {
        val token = session.getToken() ?: run {
            txtStatus.text = "Vui lòng đăng nhập trước"
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.returnLocker(
                    "Bearer $token", LockerActionRequest(lockerId = locker.lockerId)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@LockersActivity, "Đã trả tủ ${locker.code}", Toast.LENGTH_SHORT).show()
                    loadLockers()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Không thể trả tủ (có thể không phải tủ bạn thuê)"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}