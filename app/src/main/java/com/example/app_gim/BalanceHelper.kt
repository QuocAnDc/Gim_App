package com.example.app_gim

import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.app_gim.network.RetrofitClient
import kotlinx.coroutines.launch

// Hàm dùng chung: gọi API lấy số dư ví, hiển thị vào 1 TextView bất kỳ
fun loadBalanceInto(scope: LifecycleCoroutineScope, session: SessionManager, textView: TextView) {
    val token = session.getToken() ?: return
    scope.launch {
        try {
            val response = RetrofitClient.apiService.getWalletBalance("Bearer $token")
            if (response.isSuccessful) {
                val balance = response.body()?.balance ?: 0.0
                textView.text = "${"%,.0f".format(balance)}đ"
            }
        } catch (e: Exception) {
            // Nếu lỗi mạng, không hiện gì cả (tránh làm phiền người dùng ở góc màn hình)
            textView.text = ""
        }
    }
}

