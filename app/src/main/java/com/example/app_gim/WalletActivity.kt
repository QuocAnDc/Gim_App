package com.example.app_gim

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.BuyProductRequest
import com.example.app_gim.network.Product
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.TopupRequest
import kotlinx.coroutines.launch

class WalletActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var txtBalance: TextView
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        session = SessionManager(this)
        txtBalance = findViewById(R.id.txtBalance)
        container = findViewById(R.id.containerProducts)
        txtStatus = findViewById(R.id.txtStatus)

        val edtAmount = findViewById<EditText>(R.id.edtTopupAmount)
        val btnTopup = findViewById<Button>(R.id.btnTopup)

        btnTopup.setOnClickListener {
            val amountText = edtAmount.text.toString().trim()
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                txtStatus.text = "Vui lòng nhập số tiền hợp lệ"
                return@setOnClickListener
            }
            topup(amount)
        }

        loadBalance()
        loadProducts()
    }

    private fun loadBalance() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getWalletBalance("Bearer $token")
                if (response.isSuccessful) {
                    val balance = response.body()?.balance ?: 0.0
                    txtBalance.text = "Số dư: ${"%,.0f".format(balance)}đ"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi tải số dư: ${e.message}"
            }
        }
    }

    private fun topup(amount: Double) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.topupWallet("Bearer $token", TopupRequest(amount))
                if (response.isSuccessful) {
                    Toast.makeText(this@WalletActivity, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show()
                    loadBalance()
                } else {
                    txtStatus.text = "Nạp tiền thất bại"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getProducts()
                if (response.isSuccessful) {
                    val products = response.body() ?: emptyList()
                    container.removeAllViews()
                    for (p in products) {
                        container.addView(buildProductRow(p))
                    }
                } else {
                    txtStatus.text = "Không tải được sản phẩm"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildProductRow(product: Product): LinearLayout {
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
            text = "${product.name} — ${"%,.0f".format(product.price)}đ"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val txtStock = TextView(this).apply {
            text = "Còn lại: ${product.stock}"
            textSize = 13f
        }

        val btnBuy = Button(this).apply {
            text = "Mua"
            setOnClickListener { buyProduct(product) }
        }

        row.addView(txtName)
        row.addView(txtStock)
        row.addView(btnBuy)
        return row
    }

    private fun buyProduct(product: Product) {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.buyProduct(
                    "Bearer $token",
                    BuyProductRequest(productId = product.productId, quantity = 1)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@WalletActivity, "Mua ${product.name} thành công!", Toast.LENGTH_SHORT).show()
                    loadBalance()
                    loadProducts()
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Mua thất bại"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}