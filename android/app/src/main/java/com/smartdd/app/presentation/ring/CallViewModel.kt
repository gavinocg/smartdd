package com.smartdd.app.presentation.ring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.remote.websocket.WebSocketClient
import com.smartdd.app.data.repository.RingRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RingUiState(
    val isLoading: Boolean = false,
    val sessionCreated: Boolean = false,
    val sessionId: String = "",
    val roomId: String = "",
    val error: String = ""
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val ringRepository: RingRepository,
    private val webSocketClient: WebSocketClient
) : ViewModel() {
    private val _state = MutableStateFlow(RingUiState())
    val state: StateFlow<RingUiState> = _state.asStateFlow()

    fun ring(qrId: String, emisorName: String? = null) {
        viewModelScope.launch {
            _state.value = RingUiState(isLoading = true)
            when (val result = ringRepository.ring(qrId, emisorName)) {
                is Result.Success -> {
                    val session = result.data.session
                    _state.value = RingUiState(
                        sessionCreated = true,
                        sessionId = session.id,
                        roomId = session.roomId
                    )
                }
                is Result.Error -> _state.value = RingUiState(error = result.message)
            }
        }
    }

    fun respond(sessionId: String, action: String, mode: String? = null) {
        viewModelScope.launch {
            ringRepository.respond(sessionId, action, mode)
        }
    }
}
