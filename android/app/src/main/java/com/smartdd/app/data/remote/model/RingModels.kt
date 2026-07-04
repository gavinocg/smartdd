package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class RingRequest(@SerializedName("qrId") val qrId: String, @SerializedName("emisorName") val emisorName: String? = null)
data class RingResponse(val session: RingSessionDTO)
data class RingSessionDTO(val id: String, @SerializedName("roomId") val roomId: String, val status: String, val mode: String? = null)
data class RespondRequest(@SerializedName("sessionId") val sessionId: String, val action: String, val mode: String? = null)
data class RespondResponse(val success: Boolean, val status: String? = null, val session: RingSessionDTO? = null)
data class SessionResponse(val session: SessionDetailDTO)
data class SessionDetailDTO(
    val id: String, val uuid: String, val qrId: String, val emisorId: String,
    val emisorName: String?, val receptorId: String, val status: String,
    val responseMode: String?, val previewStartedAt: String?, val respondedAt: String?,
    val createdAt: String, val qr: QRSimpleDTO? = null
)
data class QRSimpleDTO(val uuid: String)
