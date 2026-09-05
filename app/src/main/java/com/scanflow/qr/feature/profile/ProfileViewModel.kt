package com.scanflow.qr.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.domain.model.AuthUser
import com.scanflow.qr.domain.model.SyncStatus
import com.scanflow.qr.domain.repository.AuthRepository
import com.scanflow.qr.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: AuthUser = AuthUser(id = "local_guest_user", email = null, displayName = "Guest User", isGuest = true),
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false,
    val feedbackMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.getCurrentUser(),
        syncRepository.getSyncStatus(),
        syncRepository.getLastSyncTime(),
        _isSyncing,
        _feedbackMessage
    ) { user, syncStatus, lastSyncTime, isSyncing, feedback ->
        ProfileUiState(
            user = user,
            syncStatus = syncStatus,
            lastSyncTime = lastSyncTime,
            isSyncing = isSyncing,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _feedbackMessage.value = "Sedang menyinkronkan data..."
            val result = syncRepository.requestSync()
            _isSyncing.value = false
            result.onSuccess { report ->
                _feedbackMessage.value = report.message
            }.onFailure { e ->
                _feedbackMessage.value = "Gagal sinkronisasi: ${e.localizedMessage ?: "Terjadi kesalahan"}"
            }
        }
    }

    fun backupData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = syncRepository.backupToCloud()
            _isSyncing.value = false
            result.onSuccess {
                _feedbackMessage.value = "Cadangan Cloud Vault berhasil dibuat"
            }.onFailure { e ->
                _feedbackMessage.value = "Gagal membuat cadangan: ${e.localizedMessage}"
            }
        }
    }

    fun restoreData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = syncRepository.restoreFromCloud()
            _isSyncing.value = false
            result.onSuccess { (scans, qrs) ->
                _feedbackMessage.value = "Berhasil memulihkan $scans riwayat scan dan $qrs QR code"
            }.onFailure { e ->
                _feedbackMessage.value = "Gagal memulihkan data: ${e.localizedMessage}"
            }
        }
    }

    fun signOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            _feedbackMessage.value = "Berhasil keluar dari akun"
            onLoggedOut()
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
