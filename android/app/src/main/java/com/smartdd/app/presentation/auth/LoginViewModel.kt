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

data class LoginUiState(
    val email: String = "", val password: String = "",
    val isLoading: Boolean = false, val error: String? = null, val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChanged(email: String) { _state.value = _state.value.copy(email = email, error = null) }
    fun onPasswordChanged(password: String) { _state.value = _state.value.copy(password = password, error = null) }

    fun login() {
        val current = _state.value
        if (current.email.isBlank()) { _state.value = current.copy(error = "Ingresa tu email"); return }
        if (current.password.length < 6) { _state.value = current.copy(error = "Mínimo 6 caracteres"); return }
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            when (val result = authRepository.login(current.email, current.password)) {
                is Result.Success -> _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                is Result.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun checkSession() { if (authRepository.isLoggedIn()) _state.value = _state.value.copy(isLoggedIn = true) }
}
