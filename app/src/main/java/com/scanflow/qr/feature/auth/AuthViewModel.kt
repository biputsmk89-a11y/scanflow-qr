package com.scanflow.qr.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.domain.model.AuthUser
import com.scanflow.qr.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successUser: AuthUser? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(
        email: String,
        pass: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.signIn(email, pass)
            result.onSuccess { user ->
                _uiState.value = AuthUiState(isLoading = false, successUser = user)
                onSuccess(user)
            }.onFailure { e ->
                val msg = e.localizedMessage ?: "Gagal masuk ke akun"
                _uiState.value = AuthUiState(isLoading = false, errorMessage = msg)
                onError(msg)
            }
        }
    }

    fun signUp(
        email: String,
        pass: String,
        displayName: String,
        onSuccess: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.signUp(email, pass, displayName)
            result.onSuccess { user ->
                _uiState.value = AuthUiState(isLoading = false, successUser = user)
                onSuccess(user)
            }.onFailure { e ->
                val msg = e.localizedMessage ?: "Gagal mendaftar akun"
                _uiState.value = AuthUiState(isLoading = false, errorMessage = msg)
                onError(msg)
            }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signInAsGuest()
            _uiState.value = AuthUiState(isLoading = false)
            onSuccess()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
