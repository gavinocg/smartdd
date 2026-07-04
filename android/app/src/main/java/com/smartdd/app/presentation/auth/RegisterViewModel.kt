package com.smartdd.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.repository.AuthRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "", val email: String = "", val password: String = "", val confirmPassword: String = "",
    val isLoading: Boolean = false, val error: String? = null, val isRegistered: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChanged(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onEmailChanged(v: String) { _state.value = _state.value.copy(email = v, error = null) }
    fun onPasswordChanged(v: String) { _state.value = _state.value.copy(password = v, error = null) }
    fun onConfirmPasswordChanged(v: String) { _state.value = _state.value.copy(confirmPassword = v, error = null) }

    fun register() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Ingresa tu nombre"); return }
        if (s.email.isBlank()) { _state.value = s.copy(error = "Ingresa tu email"); return }
        if (s.password.length < 6) { _state.value = s.copy(error = "Mínimo 6 caracteres"); return }
        if (s.password != s.confirmPassword) { _state.value = s.copy(error = "Las contraseñas no coinciden"); return }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            when (val result = authRepository.register(s.name, s.email, s.password)) {
                is Result.Success -> _state.value = _state.value.copy(isLoading = false, isRegistered = true)
                is Result.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }
}
