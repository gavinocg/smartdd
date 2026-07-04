package com.smartdd.app.data.repository

import com.smartdd.app.data.local.DebugLog
import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.*
import com.smartdd.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingRepository @Inject constructor(private val api: SmartDDApi) {
    suspend fun ring(qrId: String, emisorName: String? = null): Result<RingResponse> = try {
        val r = api.ring(RingRequest(qrId, emisorName))
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else {
            val errorBody = r.errorBody()?.string() ?: "Error al enviar timbre"
            DebugLog.e("Ring", "Error HTTP ${r.code()}: $errorBody")
            Result.Error(errorBody, r.code())
        }
    } catch (e: Exception) {
        DebugLog.e("Ring", "Excepción: ${e.message}", e)
        Result.Error(e.message ?: "Error de conexión")
    }

    suspend fun respond(sessionId: String, action: String, mode: String? = null): Result<RespondResponse> = try {
        val r = api.respond(RespondRequest(sessionId, action, mode))
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error(r.errorBody()?.string() ?: "Error al responder", r.code())
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }

    suspend fun getSession(sessionId: String): Result<SessionResponse> = try {
        val r = api.getSession(sessionId)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("Sesión no encontrada", r.code())
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }
}
