package com.example.app_gim

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.WalletTransaction
import kotlinx.coroutines.launch

class TransactionsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Lịch sử giao dịch"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtBalanceCorner).visibility = android.view.View.GONE

        session = SessionManager(this)
        container = findViewById(R.id.containerTransactions)
        txtStatus = findViewById(R.id.txtStatus)

        loadTransactions()
    }

    private fun loadTransactions() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getTransactions("Bearer $token")
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        txtStatus.text = "Chưa có giao dịch nào"
                    } else {
                        txtStatus.text = ""
                        for (item in items) container.addView(buildRow(item))
                    }
                } else {
                    txtStatus.text = "Không tải được lịch sử"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildRow(item: WalletTransaction): LinearLayout {
        val isTopup = item.type == "topup"

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundResource(R.drawable.rounded_card_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }

        val txtAmount = TextView(this).apply {
            val sign = if (isTopup) "+" else "-"
            text = "$sign${"%,.0f".format(item.amount)}đ"
            setTextColor(if (isTopup) 0xFF00E676.toInt() else 0xFFFF5252.toInt())
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val txtNote = TextView(this).apply {
            text = item.note ?: (if (isTopup) "Nạp tiền" else "Thanh toán")
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
        }

        val txtTime = TextView(this).apply {
            text = item.createdAt
            setTextColor(0xFF888888.toInt())
            textSize = 12f
        }

        row.addView(txtAmount)
        row.addView(txtNote)
        row.addView(txtTime)
        return row
    }
}