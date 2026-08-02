package com.example.app_gim.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/members/me")
    suspend fun getMyInfo(@Header("Authorization") token: String): Response<MemberInfoResponse>

    @GET("api/packages")
    suspend fun getPackages(): Response<List<PackageItem>>

    @POST("api/packages/buy")
    suspend fun buyPackage(
        @Header("Authorization") token: String,
        @Body request: BuyPackageRequest
    ): Response<BuyPackageResponse>

    @POST("api/checkin")
    suspend fun checkin(@Header("Authorization") token: String): Response<CheckinResponse>

    @GET("api/classes")
    suspend fun getClasses(): Response<List<ClassSchedule>>

    @POST("api/classes/book")
    suspend fun bookClass(
        @Header("Authorization") token: String,
        @Body request: BookClassRequest
    ): Response<BookClassResponse>
    @GET("api/pt/trainers")
    suspend fun getTrainers(): Response<List<Trainer>>

    @POST("api/pt/book")
    suspend fun bookPt(
        @Header("Authorization") token: String,
        @Body request: BookPtRequest
    ): Response<BookPtResponse>

    @GET("api/pt/my-sessions")
    suspend fun getMySessions(@Header("Authorization") token: String): Response<List<PtSession>>

    @GET("api/wallet")
    suspend fun getWalletBalance(@Header("Authorization") token: String): Response<WalletBalance>

    @POST("api/wallet/topup")
    suspend fun topupWallet(
        @Header("Authorization") token: String,
        @Body request: TopupRequest
    ): Response<SimpleMessageResponse>

    @GET("api/wallet/products")
    suspend fun getProducts(): Response<List<Product>>

    @POST("api/wallet/buy")
    suspend fun buyProduct(
        @Header("Authorization") token: String,
        @Body request: BuyProductRequest
    ): Response<BuyProductResponse>

    @GET("api/admin/users")
    suspend fun getAdminUsers(@Header("Authorization") token: String): Response<List<AdminUser>>

    @PUT("api/admin/users/{id}/status")
    suspend fun updateUserStatus(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body request: UpdateStatusRequest
    ): Response<SimpleMessageResponse>

    @DELETE("api/admin/users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<SimpleMessageResponse>

    @GET("api/admin/packages")
    suspend fun getAdminPackages(@Header("Authorization") token: String): Response<List<AdminPackage>>

    @POST("api/admin/packages")
    suspend fun createPackage(
        @Header("Authorization") token: String,
        @Body request: SavePackageRequest
    ): Response<SimpleMessageResponse>

    @PUT("api/admin/packages/{id}")
    suspend fun updatePackage(
        @Header("Authorization") token: String,
        @Path("id") packageId: Int,
        @Body request: SavePackageRequest
    ): Response<SimpleMessageResponse>

    @DELETE("api/admin/packages/{id}")
    suspend fun deletePackage(
        @Header("Authorization") token: String,
        @Path("id") packageId: Int
    ): Response<SimpleMessageResponse>

    @GET("api/admin/trainers")
    suspend fun getAdminTrainers(@Header("Authorization") token: String): Response<List<AdminTrainer>>

    @POST("api/admin/trainers")
    suspend fun createTrainer(
        @Header("Authorization") token: String,
        @Body request: SaveTrainerRequest
    ): Response<SimpleMessageResponse>

    @DELETE("api/admin/trainers/{id}")
    suspend fun deleteTrainer(
        @Header("Authorization") token: String,
        @Path("id") trainerId: Int
    ): Response<SimpleMessageResponse>

    @GET("api/admin/products")
    suspend fun getAdminProducts(@Header("Authorization") token: String): Response<List<AdminProduct>>

    @POST("api/admin/products")
    suspend fun createProduct(
        @Header("Authorization") token: String,
        @Body request: SaveProductRequest
    ): Response<SimpleMessageResponse>

    @DELETE("api/admin/products/{id}")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("id") productId: Int
    ): Response<SimpleMessageResponse>

    @GET("api/admin/classes")
    suspend fun getAdminClasses(@Header("Authorization") token: String): Response<List<AdminClass>>

    @POST("api/admin/classes")
    suspend fun createClass(
        @Header("Authorization") token: String,
        @Body request: SaveClassRequest
    ): Response<SimpleMessageResponse>

    @DELETE("api/admin/classes/{id}")
    suspend fun deleteClass(
        @Header("Authorization") token: String,
        @Path("id") classId: Int
    ): Response<SimpleMessageResponse>
    @GET("api/checkin/status")
    suspend fun getCheckinStatus(@Header("Authorization") token: String): Response<CheckinStatus>
    @PUT("api/members/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<SimpleMessageResponse>
    @PUT("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<SimpleMessageResponse>
    data class PackageItem(
        val packageId: Int,
        val name: String,
        val durationDays: Int,
        val price: Double,
        val description: String?
    )
    @GET("api/classes/my-bookings")
    suspend fun getMyBookings(@Header("Authorization") token: String): Response<List<MyBooking>>

    @PUT("api/classes/cancel/{bookingId}")
    suspend fun cancelBooking(
        @Header("Authorization") token: String,
        @Path("bookingId") bookingId: Int
    ): Response<SimpleMessageResponse>
    @GET("api/admin/classes/{classId}/schedules")
    suspend fun getClassSchedules(
        @Header("Authorization") token: String,
        @Path("classId") classId: Int
    ): Response<List<AdminSchedule>>

    @POST("api/admin/classes/{classId}/schedules")
    suspend fun createSchedule(
        @Header("Authorization") token: String,
        @Path("classId") classId: Int,
        @Body request: SaveScheduleRequest
    ): Response<SimpleMessageResponse>

    @DELETE("api/admin/schedules/{id}")
    suspend fun deleteSchedule(
        @Header("Authorization") token: String,
        @Path("id") scheduleId: Int
    ): Response<SimpleMessageResponse>
    @POST("api/packages/renew")
    suspend fun renewPackage(@Header("Authorization") token: String): Response<SimpleMessageResponse>
    @PUT("api/pt/cancel/{sessionId}")
    suspend fun cancelPtSession(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: Int
    ): Response<SimpleMessageResponse>
    @GET("api/wallet/transactions")
    suspend fun getTransactions(@Header("Authorization") token: String): Response<List<WalletTransaction>>
    @GET("api/checkin/history")
    suspend fun getCheckinHistory(@Header("Authorization") token: String): Response<List<CheckinHistoryItem>>
    @GET("api/admin/pt-sessions")
    suspend fun getAdminPtSessions(@Header("Authorization") token: String): Response<List<AdminPtSession>>

    @PUT("api/admin/pt-sessions/{id}/status")
    suspend fun updatePtSessionStatus(
        @Header("Authorization") token: String,
        @Path("id") sessionId: Int,
        @Body request: UpdatePtStatusRequest
    ): Response<SimpleMessageResponse>

    @GET("api/admin/transactions")
    suspend fun getAdminTransactions(@Header("Authorization") token: String): Response<List<AdminTransaction>>

    @GET("api/admin/checkin-logs")
    suspend fun getAdminCheckinLogs(@Header("Authorization") token: String): Response<List<AdminCheckinLog>>
}