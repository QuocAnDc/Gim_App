package com.example.app_gim

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.BuyPackageRequest
import com.example.app_gim.network.PackageItem
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
    private fun buildPackageRow(pkg: PackageItem): LinearLayout {
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

        val btnBuy = Button(this).apply {
            text = "Mua gói này"
            setOnClickListener { buyPackage(pkg) }
        }

        row.addView(txtName)
        row.addView(txtDetail)
        row.addView(btnBuy)
        return row
    }

    private fun buyPackage(pkg: PackageItem) {
        val token = session.getToken()
        if (token == null) {
            txtStatus.text = "Vui lòng đăng nhập trước"
            return
        }

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
                } else {
                    txtStatus.text = "Lỗi khi mua gói"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}