package com.example.app_gim.admin

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.R
import com.example.app_gim.SessionManager
import com.example.app_gim.network.AdminTransaction
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

class AdminTransactionsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var container: LinearLayout
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_transactions)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Giao dịch Ví điện tử"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        container = findViewById(R.id.containerItems)
        txtStatus = findViewById(R.id.txtStatus)

        loadTransactions()
    }

    private fun loadTransactions() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAdminTransactions("Bearer $token")
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
                    txtStatus.text = "Không tải được danh sách"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun buildRow(item: AdminTransaction): LinearLayout {
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

        val txtInfo = TextView(this).apply {
            val sign = if (isTopup) "+" else "-"
            text = "${item.memberName}: $sign${"%,.0f".format(item.amount)}đ\n" +
                    "${item.note ?: (if (isTopup) "Nạp tiền" else "Thanh toán")}\n${item.createdAt}"
            setTextColor(if (isTopup) 0xFF00E676.toInt() else 0xFFFF5252.toInt())
            textSize = 14f
        }
        row.addView(txtInfo)
        return row
    }
}