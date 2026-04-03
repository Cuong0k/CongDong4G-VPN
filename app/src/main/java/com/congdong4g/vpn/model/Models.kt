package com.congdong4g.vpn.model

import com.google.gson.annotations.SerializedName

// Base Response
data class BaseResponse(
    val status: String? = null,
    val message: String? = null,
    val data: Any? = null
)

// Auth
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("invite_code")
    val inviteCode: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class AuthResponse(
    val status: String? = null,
    val message: String? = null,
    val data: AuthData? = null
)

data class AuthData(
    @SerializedName("auth_data")
    val authData: String? = null,
    val token: String? = null
)

// User Info
data class UserInfoResponse(
    val status: String? = null,
    val data: UserInfo? = null
)

data class UserInfo(
    val id: Int = 0,
    val email: String = "",
    @SerializedName("transfer_enable")
    val transferEnable: Long = 0,
    @SerializedName("u")
    val upload: Long = 0,
    @SerializedName("d")
    val download: Long = 0,
    @SerializedName("expired_at")
    val expiredAt: Long? = null,
    @SerializedName("plan_id")
    val planId: Int? = null,
    val balance: Int = 0,
    @SerializedName("commission_balance")
    val commissionBalance: Int = 0,
    @SerializedName("created_at")
    val createdAt: Long = 0
)

// Subscription
data class SubscriptionResponse(
    val status: String? = null,
    val data: SubscriptionData? = null
)

data class SubscriptionData(
    @SerializedName("subscribe_url")
    val subscribeUrl: String = "",
    val token: String = "",
    @SerializedName("plan")
    val plan: Plan? = null
)

// Plans
data class PlansResponse(
    val status: String? = null,
    val data: List<Plan>? = null
)

data class Plan(
    val id: Int = 0,
    val name: String = "",
    val content: String? = null,
    @SerializedName("group_id")
    val groupId: Int = 0,
    @SerializedName("transfer_enable")
    val transferEnable: Long = 0,
    @SerializedName("device_limit")
    val deviceLimit: Int? = null,
    @SerializedName("speed_limit")
    val speedLimit: Int? = null,
    @SerializedName("month_price")
    val monthPrice: Int? = null,
    @SerializedName("quarter_price")
    val quarterPrice: Int? = null,
    @SerializedName("half_year_price")
    val halfYearPrice: Int? = null,
    @SerializedName("year_price")
    val yearPrice: Int? = null,
    @SerializedName("onetime_price")
    val onetimePrice: Int? = null,
    @SerializedName("reset_price")
    val resetPrice: Int? = null,
    val sort: Int? = null,
    @SerializedName("created_at")
    val createdAt: Long = 0,
    @SerializedName("updated_at")
    val updatedAt: Long = 0
)

// Orders
data class CreateOrderRequest(
    @SerializedName("plan_id")
    val planId: Int,
    val period: String,  // month_price, quarter_price, half_year_price, year_price, onetime_price
    @SerializedName("coupon_code")
    val couponCode: String? = null
)

data class OrderResponse(
    val status: String? = null,
    val data: String? = null  // trade_no
)

data class OrderDetailResponse(
    val status: String? = null,
    val data: OrderDetail? = null
)

data class OrderDetail(
    @SerializedName("trade_no")
    val tradeNo: String = "",
    @SerializedName("plan_id")
    val planId: Int = 0,
    @SerializedName("total_amount")
    val totalAmount: Int = 0,
    val status: Int = 0,  // 0: pending, 1: paid, 2: cancelled, 3: completed
    @SerializedName("created_at")
    val createdAt: Long = 0,
    @SerializedName("payment_info")
    val paymentInfo: PaymentInfo? = null
)

data class PaymentInfo(
    @SerializedName("bank_name")
    val bankName: String? = null,
    @SerializedName("bank_account")
    val bankAccount: String? = null,
    @SerializedName("account_name")
    val accountName: String? = null,
    val content: String? = null,
    @SerializedName("qr_code")
    val qrCode: String? = null
)

// Servers
data class ServersResponse(
    val status: String? = null,
    val data: List<Server>? = null
)

data class Server(
    val id: Int = 0,
    val name: String = "",
    val host: String = "",
    val port: Int = 0,
    val type: String = "", // shadowsocks, vmess, trojan, hysteria2
    @SerializedName("server_port")
    val serverPort: Int = 0,
    @SerializedName("rate")
    val rate: Float = 1f,
    val tags: List<String>? = null,
    @SerializedName("online")
    val online: Int = 0,
    var ping: Long = -1,
    var isSelected: Boolean = false
)

// Connection History
data class ConnectionHistory(
    val id: Long = 0,
    val serverName: String = "",
    val serverHost: String = "",
    val connectedAt: Long = 0,
    val disconnectedAt: Long? = null,
    val upload: Long = 0,
    val download: Long = 0,
    val duration: Long = 0
)

// Speed Test Result
data class SpeedTestResult(
    val downloadSpeed: Double = 0.0,  // Mbps
    val uploadSpeed: Double = 0.0,    // Mbps
    val ping: Long = 0,               // ms
    val serverName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// VPN Status
enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}
