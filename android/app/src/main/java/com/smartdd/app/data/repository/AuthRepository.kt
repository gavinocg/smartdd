package com.smartdd.app.data.repository

import com.smartdd.app.data.local.preferences.TokenManager
import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.*
import com.smartdd.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: SmartDDApi,
    private val tokenManager: TokenManager
) {
    suspend fun register(name: String, email: String, password: String): Result<RegisterResponse> = try {
        val response = api.register(RegisterRequest(name, email, password))
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            tokenManager.saveTokens(body.token, body.refreshToken)
            tokenManager.saveUserInfo(body.user.id, body.user.name, body.user.email, body.user.plan, body.user.role)
            Result.Success(body)
        } else Result.Error("Error de registro (${response.code()})", response.code())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error de conexión")
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> = try {
        val response = api.login(LoginRequest(email, password))
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            tokenManager.saveTokens(body.token, body.refreshToken)
            tokenManager.saveUserInfo(body.user.id, body.user.name, body.user.email, body.user.plan, body.user.role)
            Result.Success(body)
        } else {
            val msg = if (response.code() == 401) "Credenciales inválidas" else "Error del servidor (${response.code()})"
            Result.Error(msg, response.code())
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error de conexión")
    }

    suspend fun refreshToken(): Result<String> = try {
        val refreshToken = tokenManager.getRefreshToken() ?: return Result.Error("Sesión expirada")
        val response = api.refreshToken(RefreshRequest(refreshToken))
        if (response.isSuccessful && response.body() != null) {
            val newToken = response.body()!!.token
            tokenManager.saveTokens(newToken, refreshToken)
            Result.Success(newToken)
        } else {
            tokenManager.clear()
            Result.Error("Sesión expirada")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error al renovar sesión")
    }

    fun logout() = tokenManager.clear()
    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
    fun getCurrentUserInfo() = mapOf(
        "id" to tokenManager.getUserId(), "name" to tokenManager.getUserName(),
        "plan" to tokenManager.getUserPlan(), "role" to tokenManager.getUserRole()
    )
}
