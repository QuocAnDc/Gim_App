package com.example.app_gim.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.app_gim.LoginActivity
import com.example.app_gim.ManageUsersActivity
import com.example.app_gim.R
import com.example.app_gim.SessionManager

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val session = SessionManager(this)
        findViewById<TextView>(R.id.txtAdminWelcome).text = "Xin chào Admin, ${session.getFullName()}!"

        // Các màn quản lý con sẽ tạo ở bước sau, tạm thời để trống listener,
        // sẽ thêm dần khi từng Activity con được tạo xong.

        findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnManageUsers).setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }
        findViewById<Button>(R.id.btnManagePackages).setOnClickListener {
            startActivity(Intent(this, ManagePackagesActivity::class.java))
        }
        findViewById<Button>(R.id.btnManageTrainers).setOnClickListener {
            startActivity(Intent(this, ManageTrainersActivity::class.java))
        }
        findViewById<Button>(R.id.btnManageProducts).setOnClickListener {
            startActivity(Intent(this, ManageProductsActivity::class.java))
        }
        findViewById<Button>(R.id.btnManageClasses).setOnClickListener {
            startActivity(Intent(this, ManageClassesActivity::class.java))
        }
        findViewById<Button>(R.id.btnManagePt).setOnClickListener {
            startActivity(Intent(this, ManagePtSessionsActivity::class.java))
        }
        findViewById<Button>(R.id.btnAdminTransactions).setOnClickListener {
            startActivity(Intent(this, AdminTransactionsActivity::class.java))
        }
        findViewById<Button>(R.id.btnAdminCheckinLogs).setOnClickListener {
            startActivity(Intent(this, AdminCheckinLogsActivity::class.java))
        }
    }
}