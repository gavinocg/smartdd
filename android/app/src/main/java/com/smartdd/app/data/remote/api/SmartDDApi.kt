package com.smartdd.app.data.remote.api

import com.smartdd.app.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

interface SmartDDApi {
    companion object {
        const val BASE_URL = "http://192.168.100.101:8000"
    }

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<RefreshResponse>

    @GET("api/v1/auth/me")
    suspend fun me(): Response<UserResponse>

    @POST("api/v1/qr")
    suspend fun createQR(@Body request: CreateQRRequest): Response<CreateQRResponse>

    @GET("api/v1/qr")
    suspend fun listQRs(): Response<QRListResponse>

    @GET("api/v1/qr/{uuid}")
    suspend fun getQR(@Path("uuid") uuid: String): Response<QRDetailResponse>

    @POST("api/v1/qr/{uuid}/validate")
    suspend fun validateQR(@Path("uuid") uuid: String, @Body request: ValidateQRRequest): Response<ValidateQRResponse>

    @DELETE("api/v1/qr/{uuid}")
    suspend fun deleteQR(@Path("uuid") uuid: String): Response<Unit>

    @POST("api/v1/ring")
    suspend fun ring(@Body request: RingRequest): Response<RingResponse>

    @POST("api/v1/respond")
    suspend fun respond(@Body request: RespondRequest): Response<RespondResponse>

    @GET("api/v1/session/{id}")
    suspend fun getSession(@Path("id") sessionId: String): Response<SessionResponse>

    @GET("api/v1/user/config")
    suspend fun getConfig(): Response<ConfigResponse>

    @PUT("api/v1/user/config")
    suspend fun updateConfig(@Body request: UpdateConfigRequest): Response<ConfigResponse>

    @GET("api/v1/user/plan")
    suspend fun getPlan(): Response<PlanResponse>

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<RegisterDeviceResponse>

    @POST("api/v1/user/upgrade")
    suspend fun upgradePlan(@Body request: UpgradeRequest): Response<UpgradeResponse>
}
