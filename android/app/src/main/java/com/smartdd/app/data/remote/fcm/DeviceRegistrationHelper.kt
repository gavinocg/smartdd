package com.smartdd.app.data.remote.fcm

import android.util.Log
import com.smartdd.app.data.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRegistrationHelper @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    fun registerToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = deviceRepository.registerToken(token)
            Log.d("DeviceRegistration", "Registro FCM: $result")
        }
    }
}
