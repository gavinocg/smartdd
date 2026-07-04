package com.smartdd.app.presentation.qr.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.repository.QRRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val isValidating: Boolean = false,
    val isValid: Boolean? = null,
    val distance: Double = 0.0,
    val message: String = "",
    val qrUuid: String = ""
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val qrRepository: QRRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    fun validateQR(uuid: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            _state.value = ScannerUiState(isValidating = true, qrUuid = uuid)
            when (val result = qrRepository.validateQR(uuid, lat, lng)) {
                is Result.Success -> _state.value = _state.value.copy(
                    isValidating = false, isValid = result.data.valid,
                    distance = result.data.distance, message = result.data.message
                )
                is Result.Error -> _state.value = _state.value.copy(isValidating = false, isValid = false, message = result.message)
            }
        }
    }
}
