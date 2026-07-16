package com.example.app_gim.network

// Dữ liệu gửi lên khi đăng ký
data class RegisterRequest(
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val password: String
)

// Dữ liệu server trả về sau khi đăng ký thành công
data class RegisterResponse(
    val message: String,
    val userId: Int
)

// Dữ liệu gửi lên khi đăng nhập
data class LoginRequest(
    val phone: String,
    val password: String
)

// Dữ liệu server trả về sau khi đăng nhập thành công
data class LoginResponse(
    val token: String,
    val user: UserInfo
)

data class UserInfo(
    val userId: Int,
    val fullName: String,
    val role: String
)

// Dữ liệu lỗi server trả về (vd: sai mật khẩu, trùng SĐT)
data class ErrorResponse(
    val error: String
)
data class MemberInfoResponse(
    val fullName: String,
    val phone: String,
    val email: String?,
    val cardId: Int?,
    val cardCode: String?,
    val activationDate: String?,
    val expiryDate: String?,
    val cardStatus: String?,
    val packageName: String?,
    val price: Double?
)
data class PackageItem(
    val packageId: Int,
    val name: String,
    val durationDays: Int,
    val price: Double
)

data class BuyPackageRequest(
    val packageId: Int
)

data class BuyPackageResponse(
    val message: String,
    val cardId: Int,
    val cardCode: String
)
data class CheckinResponse(
    val message: String,
    val type: String
)
data class ClassSchedule(
    val scheduleId: Int,
    val className: String,
    val capacity: Int,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val bookedCount: Int
)

data class BookClassRequest(
    val scheduleId: Int
)

data class BookClassResponse(
    val message: String
)
data class Trainer(
    val trainerId: Int,
    val fullName: String,
    val phone: String?,
    val specialty: String?,
    val rating: Double
)

data class BookPtRequest(
    val trainerId: Int,
    val scheduledTime: String,
    val note: String? = null
)

data class BookPtResponse(
    val message: String
)

data class PtSession(
    val sessionId: Int,
    val trainerName: String,
    val scheduledTime: String,
    val status: String,
    val note: String?
)
data class Locker(
    val lockerId: Int,
    val code: String,
    val status: String
)

data class LockerActionRequest(
    val lockerId: Int
)

data class LockerActionResponse(
    val message: String
)
data class WalletBalance(
    val balance: Double
)

data class TopupRequest(
    val amount: Double
)

data class SimpleMessageResponse(
    val message: String
)

data class Product(
    val productId: Int,
    val name: String,
    val price: Double,
    val stock: Int
)

data class BuyProductRequest(
    val productId: Int,
    val quantity: Int = 1
)

data class BuyProductResponse(
    val message: String,
    val totalCost: Double
)