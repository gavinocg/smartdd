package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlanResponse(
    val plan: String, @SerializedName("qrLimit") val qrLimit: Int,
    @SerializedName("deviceLimit") val deviceLimit: Int,
    @SerializedName("currentQRs") val currentQRs: Int,
    @SerializedName("currentDevices") val currentDevices: Int
)
data class UpgradeRequest(@SerializedName("purchaseToken") val purchaseToken: String, @SerializedName("productId") val productId: String)
data class UpgradeResponse(val user: UserDTO, val message: String)
