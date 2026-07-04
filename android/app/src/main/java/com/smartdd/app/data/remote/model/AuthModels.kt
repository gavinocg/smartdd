package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class RefreshRequest(@SerializedName("refreshToken") val refreshToken: String)

data class RegisterResponse(val user: UserDTO, val token: String, @SerializedName("refreshToken") val refreshToken: String)
data class LoginResponse(val user: UserDTO, val token: String, @SerializedName("refreshToken") val refreshToken: String)
data class RefreshResponse(val token: String)
data class UserResponse(val user: UserDTO)

data class UserDTO(
    val id: String, val name: String, val email: String, val plan: String,
    val role: String, val active: Boolean? = null, val createdAt: String? = null,
    val config: UserConfigDTO? = null
)
