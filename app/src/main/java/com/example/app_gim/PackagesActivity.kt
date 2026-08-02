package com.example.app_gim

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.ApiService
import com.example.app_gim.network.BuyPackageRequest
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class PackagesActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packages)

        session = SessionManager(this)
        container = findViewById(R.id.containerPackages)
        txtStatus = findViewById(R.id.txtStatus)

        loadPackages()
        findViewById<TextView>(R.id.txtToolbarTitle).text = "Gói tập"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        loadBalanceInto(lifecycleScope, session, findViewById(R.id.txtBalanceCorner))
    }

    private fun loadPackages() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPackages()
                if (response.isSuccessful) {
                    val packages = response.body() ?: emptyList()
                    container.removeAllViews()
                    for (pkg in packages) {
                        container.addView(buildPackageRow(pkg))
                    }
                } else {
                    txtStatus.text = "Không tải được danh sách gói"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    // Tạo 1 "khối" hiển thị cho mỗi gói tập: tên + giá + nút Mua
    private fun buildPackageRow(pkg: ApiService.PackageItem): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF0F0F0.toInt())
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val txtName = TextView(this).apply {
            text = pkg.name
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val txtDetail = TextView(this).apply {
            text = "Thời hạn: ${pkg.durationDays} ngày — Giá: ${"%,.0f".format(pkg.price)}đ"
            textSize = 14f
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
        }

        val btnViewDetail = Button(this).apply {
            text = "Xem chi tiết"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showPackageDetail(pkg) }
        }

        val btnBuy = Button(this).apply {
            text = "Mua gói này"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
            setOnClickListener { buyPackage(pkg) }
        }

        btnRow.addView(btnViewDetail)
        btnRow.addView(btnBuy)

        row.addView(txtName)
        row.addView(txtDetail)
        row.addView(btnRow)
        return row
    }

    private fun showPackageDetail(pkg: ApiService.PackageItem) {
        val pricePerDay = pkg.price / pkg.durationDays
        val message = buildString {
            append("Tên gói: ${pkg.name}\n\n")
            append("Thời hạn: ${pkg.durationDays} ngày\n")
            append("Giá: ${"%,.0f".format(pkg.price)}đ\n")
            append("Trung bình: ${"%,.0f".format(pricePerDay)}đ/ngày\n\n")
            append("Mô tả: ${pkg.description ?: "Chưa có mô tả"}")
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Chi tiết gói tập")
            .setMessage(message)
            .setPositiveButton("Mua ngay") { _, _ -> buyPackage(pkg) }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun buyPackage(pkg: ApiService.PackageItem) {
        val token = session.getToken()
        if (token == null) {
            txtStatus.text = "Vui lòng đăng nhập trước"
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận mua gói")
            .setMessage("Bạn có chắc muốn mua \"${pkg.name}\" với giá ${"%,.0f".format(pkg.price)}đ không? Số tiền sẽ được trừ từ ví.")
            .setPositiveButton("Đồng ý") { _, _ -> confirmBuyPackage(pkg, token) }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun confirmBuyPackage(pkg: ApiService.PackageItem, token: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.buyPackage(
                    "Bearer $token",
                    BuyPackageRequest(packageId = pkg.packageId)
                )
                if (response.isSuccessful) {
                    val result = response.body()
                    Toast.makeText(
                        this@PackagesActivity,
                        "Mua thành công! Mã thẻ: ${result?.cardCode}",
                        Toast.LENGTH_LONG
                    ).show()
                    txtStatus.text = "Mua gói \"${pkg.name}\" thành công!"
                    loadBalanceInto(lifecycleScope, session, findViewById(R.id.txtBalanceCorner))
                } else {
                    val rawError = response.errorBody()?.string()
                    val friendlyMsg = try {
                        val json = org.json.JSONObject(rawError ?: "{}")
                        json.optString("error", "Không thể mua gói, vui lòng thử lại")
                    } catch (e: Exception) {
                        "Không thể mua gói, vui lòng thử lại"
                    }
                    txtStatus.text = friendlyMsg
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}