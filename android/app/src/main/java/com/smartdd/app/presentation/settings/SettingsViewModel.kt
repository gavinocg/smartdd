package com.smartdd.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.UpdateConfigRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultMode: String = "CHAT", val chatEnabled: Boolean = true,
    val audioEnabled: Boolean = true, val videoEnabled: Boolean = true,
    val timeoutSeconds: Int = 60, val isLoading: Boolean = false,
    val error: String? = null, val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val api: SmartDDApi) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun loadConfig() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val response = api.getConfig()
                if (response.isSuccessful && response.body() != null) {
                    val c = response.body()!!.config
                    _state.value = SettingsUiState(
                        defaultMode = c.defaultMode, chatEnabled = c.chatEnabled,
                        audioEnabled = c.audioEnabled, videoEnabled = c.videoEnabled,
                        timeoutSeconds = c.timeoutSeconds ?: 60
                    )
                }
            } catch (e: Exception) { _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun setDefaultMode(mode: String) { _state.value = _state.value.copy(defaultMode = mode, saved = false) }

    fun toggleChat(checked: Boolean) {
        _state.value = _state.value.copy(chatEnabled = checked, saved = false)
        if (!checked && !_state.value.audioEnabled && !_state.value.videoEnabled)
            _state.value = _state.value.copy(error = "Debe tener al menos una opción activa", chatEnabled = true)
    }

    fun toggleAudio(checked: Boolean) {
        _state.value = _state.value.copy(audioEnabled = checked, saved = false)
        if (!checked && !_state.value.chatEnabled && !_state.value.videoEnabled)
            _state.value = _state.value.copy(error = "Debe tener al menos una opción activa", audioEnabled = true)
    }

    fun toggleVideo(checked: Boolean) {
        _state.value = _state.value.copy(videoEnabled = checked, saved = false)
        if (!checked && !_state.value.chatEnabled && !_state.value.audioEnabled)
            _state.value = _state.value.copy(error = "Debe tener al menos una opción activa", videoEnabled = true)
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val r = api.updateConfig(UpdateConfigRequest(
                    defaultMode = _state.value.defaultMode, chatEnabled = _state.value.chatEnabled,
                    audioEnabled = _state.value.audioEnabled, videoEnabled = _state.value.videoEnabled,
                    timeoutSeconds = _state.value.timeoutSeconds
                ))
                if (r.isSuccessful) _state.value = _state.value.copy(isLoading = false, saved = true)
                else _state.value = _state.value.copy(isLoading = false, error = "Error al guardar")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = e.message) }
        }
    }
}
