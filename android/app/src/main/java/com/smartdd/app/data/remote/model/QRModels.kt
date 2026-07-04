package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class CreateQRRequest(val lat: Double, val lng: Double, val radius: Int? = 50)
data class CreateQRResponse(val qr: QRDTO)
data class QRDTO(
    val id: String, val uuid: String, val lat: Double, val lng: Double,
    @SerializedName("radiusMeters") val radiusMeters: Int, val active: Boolean,
    val createdAt: String, @SerializedName("imageUrl") val imageUrl: String? = null
)
data class QRListResponse(val qrs: List<QRDTO>)
data class QRDetailResponse(val qr: QRDTO)
data class ValidateQRRequest(val lat: Double, val lng: Double)
data class ValidateQRResponse(val valid: Boolean, val distance: Double, @SerializedName("radiusMeters") val radiusMeters: Int, val message: String)
