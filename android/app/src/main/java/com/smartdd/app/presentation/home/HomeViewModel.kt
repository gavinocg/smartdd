package com.smartdd.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.smartdd.app.data.remote.model.QRDTO
import com.smartdd.app.data.repository.AuthRepository
import com.smartdd.app.data.repository.DeviceRepository
import com.smartdd.app.data.repository.QRRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isReceptor: Boolean = false, val qrList: List<QRDTO> = emptyList(),
    val isLoading: Boolean = false, val error: String? = null,
    val qrLimit: Int = 1, val currentQRCount: Int = 0, val userName: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val qrRepository: QRRepository,
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        registerFCMToken()
    }

    private fun registerFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { token ->
                    viewModelScope.launch { deviceRepository.registerToken(token) }
                }
            }
        }
    }

    fun loadUserInfo() {
        val info = authRepository.getCurrentUserInfo()
        val plan = info["plan"] as? String ?: "FREE"
        _state.value = _state.value.copy(
            userName = info["name"] as? String ?: "",
            qrLimit = if (plan == "FREE") 1 else Int.MAX_VALUE
        )
    }

    fun loadQRs() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = qrRepository.listQRs()) {
                is Result.Success -> _state.value = _state.value.copy(
                    qrList = result.data.qrs, isLoading = false,
                    currentQRCount = result.data.qrs.count { it.active }
                )
                is Result.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun setRole(isReceptor: Boolean) {
        _state.value = _state.value.copy(isReceptor = isReceptor)
        if (isReceptor) loadQRs()
    }

    fun deleteQR(uuid: String) {
        viewModelScope.launch { qrRepository.deleteQR(uuid); loadQRs() }
    }
}
