package com.example.app_gim

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val session = SessionManager(this)
        val txtWelcome = findViewById<TextView>(R.id.txtWelcome)

        loadMemberInfo(session, txtWelcome)
        val btnPackages = findViewById<Button>(R.id.btnPackages)
        btnPackages.setOnClickListener {
            startActivity(Intent(this, PackagesActivity::class.java))
        }
        val btnCheckin = findViewById<Button>(R.id.btnCheckin)
        btnCheckin.setOnClickListener {
            confirmCheckin(session)
        }

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        val btnClasses = findViewById<Button>(R.id.btnClasses)
        btnClasses.setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java))
        }
        val btnPt = findViewById<Button>(R.id.btnPt)
        btnPt.setOnClickListener {
            startActivity(Intent(this, PtActivity::class.java))
        }

        val btnWallet = findViewById<Button>(R.id.btnWallet)
        btnWallet.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }
        findViewById<Button>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<Button>(R.id.btnRenew).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận gia hạn")
                .setMessage("Bạn có chắc muốn gia hạn gói tập không? Số tiền sẽ được trừ từ ví.")
                .setPositiveButton("Đồng ý") { _, _ -> renewPackage(session, txtWelcome) }
                .setNegativeButton("Huỷ", null)
                .show()
        }
        findViewById<Button>(R.id.btnCheckinHistory).setOnClickListener {
            startActivity(Intent(this, CheckinHistoryActivity::class.java))
        }

    }
    override fun onResume() {
        super.onResume()
        loadBalanceInto(lifecycleScope, SessionManager(this), findViewById(R.id.txtBalanceCorner))
    }

    private fun loadMemberInfo(session: SessionManager, txtWelcome: TextView) {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyInfo("Bearer $token")
                if (response.isSuccessful) {
                    val info = response.body()
                    txtWelcome.text = buildString {
                        if (info?.cardCode != null) {
                            append("Chào mừng, ${info?.fullName}!\n")
                            append("Mã hội viên: ${info?.memberCode}\n\n")
                            append("Mã thẻ: ${info.cardCode}\n")
                            append("Gói: ${info.packageName}\n")
                            append("Còn lại: ${daysRemaining(info.expiryDate)} ngày\n")
                            append("Trạng thái: ${info.cardStatus}\n")
                            append("Hết hạn: ${formatDate(info.expiryDate)}")
                        } else {
                            append("Bạn chưa có thẻ tập nào.\nHãy mua gói tập để bắt đầu!")
                        }
                    }

                    val btnRenew = findViewById<Button>(R.id.btnRenew)
                    if (info?.cardCode != null && daysRemaining(info.expiryDate) <= 7) {
                        btnRenew.visibility = android.view.View.VISIBLE
                    } else {
                        btnRenew.visibility = android.view.View.GONE
                    }
                } else {
                    txtWelcome.text = "Không tải được thông tin thẻ"
                }
            } catch (e: Exception) {
                txtWelcome.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun formatDate(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "-"
        return try {
            val datePart = isoString.substring(0, 10)
            val parts = datePart.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month - 1, day)

            val weekdayNames = arrayOf(
                "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư",
                "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
            )
            val weekdayIndex = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
            val weekdayName = weekdayNames[weekdayIndex]

            "$weekdayName, ${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (e: Exception) {
            isoString
        }
    }

    private fun doCheckin(session: SessionManager) {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.checkin("Bearer $token")
                if (response.isSuccessful) {
                    val result = response.body()
                    val currentTime = java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date())

                    val title = if (result?.type == "checkin") "✅ Check-in thành công" else "👋 Check-out thành công"

                    android.app.AlertDialog.Builder(this@HomeActivity)
                        .setTitle(title)
                        .setMessage("${result?.message}\n\nThời gian: $currentTime")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Không thể check-in"
                    android.app.AlertDialog.Builder(this@HomeActivity)
                        .setTitle("Không thể check-in")
                        .setMessage(errorMsg)
                        .setPositiveButton("Đóng", null)
                        .show()
                }
            } catch (e: Exception) {
                android.app.AlertDialog.Builder(this@HomeActivity)
                    .setTitle("Lỗi kết nối")
                    .setMessage(e.message)
                    .setPositiveButton("Đóng", null)
                    .show()
            }
        }
    }
    private fun confirmCheckin(session: SessionManager) {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val infoResponse = RetrofitClient.apiService.getMyInfo("Bearer $token")
                if (infoResponse.body()?.cardCode == null) {
                    android.app.AlertDialog.Builder(this@HomeActivity)
                        .setTitle("Chưa có gói tập")
                        .setMessage("Bạn cần mua gói tập trước khi có thể Check-in/Check-out.")
                        .setPositiveButton("Mua gói ngay") { _, _ ->
                            startActivity(android.content.Intent(this@HomeActivity, PackagesActivity::class.java))
                        }
                        .setNegativeButton("Đóng", null)
                        .show()
                    return@launch
                }

                val statusResponse = RetrofitClient.apiService.getCheckinStatus("Bearer $token")
                val isCheckedIn = statusResponse.body()?.isCheckedIn ?: false

                val title = if (isCheckedIn) "Xác nhận Check-out" else "Xác nhận Check-in"
                val message = if (isCheckedIn)
                    "Bạn có chắc chắn muốn Check-out không?"
                else
                    "Bạn có chắc chắn muốn Check-in không?"

                android.app.AlertDialog.Builder(this@HomeActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Đồng ý") { _, _ -> doCheckin(session) }
                    .setNegativeButton("Huỷ", null)
                    .show()
            } catch (e: Exception) {
                android.app.AlertDialog.Builder(this@HomeActivity)
                    .setTitle("Lỗi kết nối")
                    .setMessage(e.message)
                    .setPositiveButton("Đóng", null)
                    .show()
            }
        }
    }
    // Tính số ngày còn lại từ ngày hết hạn đến hôm nay
    private fun daysRemaining(isoString: String?): Int {
        if (isoString.isNullOrEmpty()) return 0
        return try {
            val datePart = isoString.substring(0, 10)
            val parts = datePart.split("-")
            val expiryCalendar = java.util.Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayCalendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val diffMillis = expiryCalendar.timeInMillis - todayCalendar.timeInMillis
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
            if (diffDays < 0) 0 else diffDays
        } catch (e: Exception) {
            0
        }
    }private fun renewPackage(session: SessionManager, txtWelcome: TextView) {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.renewPackage("Bearer $token")
                if (response.isSuccessful) {
                    Toast.makeText(this@HomeActivity, "Gia hạn thành công!", Toast.LENGTH_SHORT).show()
                    loadMemberInfo(session, txtWelcome)
                    loadBalanceInto(lifecycleScope, session, findViewById(R.id.txtBalanceCorner))
                } else {
                    val rawError = response.errorBody()?.string()
                    val friendlyMsg = try {
                        org.json.JSONObject(rawError ?: "{}").optString("error", "Không thể gia hạn")
                    } catch (e: Exception) {
                        "Không thể gia hạn"
                    }
                    Toast.makeText(this@HomeActivity, friendlyMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

}