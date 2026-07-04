package com.smartdd.app.presentation.qr.generate

import android.location.Location
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

data class GenerateQRUiState(
    val lat: Double? = null, val lng: Double? = null, val radius: Int = 50,
    val isLoading: Boolean = false, val error: String? = null, val qrImageUrl: String? = null
)

@HiltViewModel
class GenerateQRViewModel @Inject constructor(
    private val qrRepository: QRRepository
) : ViewModel() {
    private val _state = MutableStateFlow(GenerateQRUiState())
    val state: StateFlow<GenerateQRUiState> = _state.asStateFlow()

    fun setLocation(location: Location) {
        _state.value = _state.value.copy(lat = location.latitude, lng = location.longitude)
    }

    fun setRadius(radius: Int) { _state.value = _state.value.copy(radius = radius) }

    fun generate() {
        val current = _state.value
        if (current.lat == null || current.lng == null) {
            _state.value = current.copy(error = "Esperando ubicación GPS...")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            when (val result = qrRepository.createQR(current.lat, current.lng, current.radius)) {
                is Result.Success -> _state.value = _state.value.copy(isLoading = false, qrImageUrl = result.data.qr.imageUrl)
                is Result.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }
}
