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
    val memberCode: String?,
    val dob: String?,
    val gender: String?,
    val address: String?,
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
    val price: Double,
    val description: String?
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
    val rating: Double,
    val pricePerSession: Double
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
data class AdminUser(
    val userId: Int,
    val fullName: String,
    val phone: String,
    val email: String?,
    val status: String,
    val roleName: String
)

data class UpdateStatusRequest(
    val status: String
)
data class AdminPackage(
    val packageId: Int,
    val name: String,
    val durationDays: Int,
    val price: Double
)

data class SavePackageRequest(
    val name: String,
    val durationDays: Int,
    val price: Double
)
data class AdminTrainer(
    val trainerId: Int,
    val fullName: String,
    val phone: String?,
    val specialty: String?,
    val pricePerSession: Double?
)

data class SaveTrainerRequest(
    val fullName: String,
    val phone: String?,
    val specialty: String?,
    val pricePerSession: Double?
)
data class AdminProduct(
    val productId: Int,
    val name: String,
    val price: Double,
    val stock: Int
)

data class SaveProductRequest(
    val name: String,
    val price: Double,
    val stock: Int
)
data class AdminClass(
    val classId: Int,
    val name: String,
    val description: String?,
    val capacity: Int
)

data class SaveClassRequest(
    val name: String,
    val description: String?,
    val capacity: Int
)
data class CheckinStatus(
    val isCheckedIn: Boolean
)
data class UpdateProfileRequest(
    val fullName: String?,
    val email: String?,
    val dob: String?,
    val gender: String?,
    val address: String?
)
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
data class MyBooking(
    val bookingId: Int,
    val className: String,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val status: String
)
data class AdminSchedule(
    val scheduleId: Int,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val status: String
)

data class SaveScheduleRequest(
    val startTime: String,
    val endTime: String,
    val room: String?
)
data class WalletTransaction(
    val txId: Int,
    val amount: Double,
    val type: String,
    val note: String?,
    val createdAt: String
)
data class CheckinHistoryItem(
    val logId: Int,
    val checkinTime: String,
    val checkoutTime: String?
)
data class AdminPtSession(
    val sessionId: Int,
    val memberName: String,
    val trainerName: String,
    val scheduledTime: String,
    val status: String,
    val note: String?
)

data class UpdatePtStatusRequest(
    val status: String
)

data class AdminTransaction(
    val txId: Int,
    val memberName: String,
    val amount: Double,
    val type: String,
    val note: String?,
    val createdAt: String
)

data class AdminCheckinLog(
    val logId: Int,
    val memberName: String,
    val checkinTime: String,
    val checkoutTime: String?
)