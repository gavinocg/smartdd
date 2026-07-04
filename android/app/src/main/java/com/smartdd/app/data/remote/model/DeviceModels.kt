package com.smartdd.app.data.remote.model

data class RegisterDeviceRequest(
    val token: String,
    val platform: String = "android"
)
data class RegisterDeviceResponse(val success: Boolean)
