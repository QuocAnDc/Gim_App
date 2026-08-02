package com.example.app_gim

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_gim.network.ChangePasswordRequest
import com.example.app_gim.network.RetrofitClient
import com.example.app_gim.network.UpdateProfileRequest
import kotlinx.coroutines.launch
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var edtFullName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtDob: EditText
    private lateinit var edtGender: EditText
    private lateinit var edtAddress: EditText
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.txtToolbarTitle).text = "Trang cá nhân"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        session = SessionManager(this)
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtDob = findViewById(R.id.edtDob)
        edtGender = findViewById(R.id.edtGender)
        edtAddress = findViewById(R.id.edtAddress)
        txtStatus = findViewById(R.id.txtStatus)

        edtDob.setOnClickListener { pickDate() }

        loadProfile()

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveProfile()
        }
        val edtOldPassword = findViewById<EditText>(R.id.edtOldPassword)
        val edtNewPassword = findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)

        findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
            val oldPass = edtOldPassword.text.toString().trim()
            val newPass = edtNewPassword.text.toString().trim()
            val confirmPass = edtConfirmPassword.text.toString().trim()

            when {
                oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty() -> {
                    txtStatus.text = "Vui lòng nhập đủ 3 trường mật khẩu"
                }
                newPass.length < 6 -> {
                    txtStatus.text = "Mật khẩu mới phải có ít nhất 6 ký tự"
                }
                newPass != confirmPass -> {
                    txtStatus.text = "Mật khẩu mới nhập lại không khớp"
                }
                else -> {
                    changePassword(oldPass, newPass, edtOldPassword, edtNewPassword, edtConfirmPassword)
                }
            }
        }
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }git push origin main
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, R.style.PickerDialogTheme, { _, year, month, day ->
            edtDob.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadProfile() {
        val token = session.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyInfo("Bearer $token")
                if (response.isSuccessful) {
                    val info = response.body()
                    findViewById<TextView>(R.id.txtMemberCode).text = info?.memberCode ?: "-"
                    findViewById<TextView>(R.id.txtPhone).text = info?.phone ?: "-"
                    findViewById<TextView>(R.id.txtPhone).text = info?.phone ?: "-"
                    edtFullName.setText(info?.fullName ?: "")
                    edtEmail.setText(info?.email ?: "")
                    edtDob.setText(info?.dob ?: "")
                    edtGender.setText(info?.gender ?: "")
                    edtAddress.setText(info?.address ?: "")
                } else {
                    txtStatus.text = "Không tải được thông tin"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    private fun saveProfile() {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateProfile(
                    "Bearer $token",
                    UpdateProfileRequest(
                        fullName = edtFullName.text.toString().trim().ifEmpty { null },
                        email = edtEmail.text.toString().trim().ifEmpty { null },
                        dob = edtDob.text.toString().trim().ifEmpty { null },
                        gender = edtGender.text.toString().trim().ifEmpty { null },
                        address = edtAddress.text.toString().trim().ifEmpty { null }
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    txtStatus.text = "Không thể cập nhật thông tin"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
    private fun changePassword(
        oldPass: String, newPass: String,
        edtOld: EditText, edtNew: EditText, edtConfirm: EditText
    ) {
        val token = session.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.changePassword(
                    "Bearer $token",
                    ChangePasswordRequest(oldPassword = oldPass, newPassword = newPass)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                    edtOld.text.clear()
                    edtNew.text.clear()
                    edtConfirm.text.clear()
                    txtStatus.text = ""
                } else {
                    txtStatus.text = response.errorBody()?.string() ?: "Đổi mật khẩu thất bại"
                }
            } catch (e: Exception) {
                txtStatus.text = "Lỗi kết nối: ${e.message}"
            }
        }
    }
}