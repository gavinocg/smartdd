package com.smartdd.app.data.repository

import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.RegisterDeviceRequest
import com.smartdd.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(private val api: SmartDDApi) {
    suspend fun registerToken(token: String): Result<Boolean> = try {
        val r = api.registerDevice(RegisterDeviceRequest(token))
        if (r.isSuccessful && r.body()?.success == true) Result.Success(true)
        else Result.Error("Error al registrar dispositivo")
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }
}
