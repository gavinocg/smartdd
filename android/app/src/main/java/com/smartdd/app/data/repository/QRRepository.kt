package com.smartdd.app.data.repository

import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.*
import com.smartdd.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRRepository @Inject constructor(private val api: SmartDDApi) {
    suspend fun createQR(lat: Double, lng: Double, radius: Int = 50): Result<CreateQRResponse> = try {
        val r = api.createQR(CreateQRRequest(lat, lng, radius))
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error(r.errorBody()?.string() ?: "Error al crear QR", r.code())
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }

    suspend fun listQRs(): Result<QRListResponse> = try {
        val r = api.listQRs()
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error(r.errorBody()?.string() ?: "Error al listar QRs")
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }

    suspend fun getQR(uuid: String): Result<QRDetailResponse> = try {
        val r = api.getQR(uuid)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("QR no encontrado", r.code())
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }

    suspend fun validateQR(uuid: String, lat: Double, lng: Double): Result<ValidateQRResponse> = try {
        val r = api.validateQR(uuid, ValidateQRRequest(lat, lng))
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error(r.errorBody()?.string() ?: "Error al validar QR")
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }

    suspend fun deleteQR(uuid: String): Result<Unit> = try {
        val r = api.deleteQR(uuid)
        if (r.isSuccessful) Result.Success(Unit)
        else Result.Error(r.errorBody()?.string() ?: "Error al eliminar QR")
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }
}
