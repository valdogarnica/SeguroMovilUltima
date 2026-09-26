package com.devg2studios.seguromovilultima.ui.views.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devg2studios.seguromovilultima.data.repository.AuthRepository
import com.devg2studios.seguromovilultima.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registeredSuccessfully: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun register() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Correo requerido y contraseña de al menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.register(state.email, state.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, registeredSuccessfully = true)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}