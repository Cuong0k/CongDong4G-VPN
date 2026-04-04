package com.congdong4g.vpn.api

import com.congdong4g.vpn.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    companion object {
        const val BASE_URL = "https://congdong4g.com/"
    }

    // Auth
    @POST("api/v1/passport/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/passport/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/passport/auth/forget")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<BaseResponse>

    // User - dùng auth_data query param
    @GET("api/v1/user/info")
    suspend fun getUserInfo(@Query("auth_data") authData: String): Response<UserInfoResponse>

    @GET("api/v1/user/getSubscribe")
    suspend fun getSubscription(@Query("auth_data") authData: String): Response<SubscriptionResponse>

    // Plans
    @GET("api/v1/user/plan/fetch")
    suspend fun getPlans(@Query("auth_data") authData: String): Response<PlansResponse>

    // Orders
    @POST("api/v1/user/order/save")
    suspend fun createOrder(
        @Query("auth_data") authData: String,
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    @GET("api/v1/user/order/detail")
    suspend fun getOrderDetail(
        @Query("auth_data") authData: String,
        @Query("trade_no") tradeNo: String
    ): Response<OrderDetailResponse>

    // Servers
    @GET("api/v1/user/server/fetch")
    suspend fun getServers(@Query("auth_data") authData: String): Response<ServersResponse>

    // Subscribe - dùng auth_data thay vì token
    @GET("api/v1/client/subscribe")
    suspend fun getSubscribeConfig(@Query("auth_data") authData: String): Response<SubscribeConfigResponse>
}

// Response model cho subscribe config (trả về JSON với subscribe_url)
data class SubscribeConfigResponse(
    val status: String? = null,
    val data: SubscribeConfigData? = null
)

data class SubscribeConfigData(
    val subscribeUrl: String = "",
    val subscribe: String = ""  // base64 config content
)
