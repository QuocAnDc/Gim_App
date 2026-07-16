package com.example.app_gim.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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

    @GET("api/lockers")
    suspend fun getLockers(): Response<List<Locker>>

    @POST("api/lockers/rent")
    suspend fun rentLocker(
        @Header("Authorization") token: String,
        @Body request: LockerActionRequest
    ): Response<LockerActionResponse>

    @POST("api/lockers/return")
    suspend fun returnLocker(
        @Header("Authorization") token: String,
        @Body request: LockerActionRequest
    ): Response<LockerActionResponse>

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
}