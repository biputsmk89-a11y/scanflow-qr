package com.scanflow.qr.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.model.AuthUser
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.SyncStatus
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.AuthRepository
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import com.scanflow.qr.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: AuthUser = AuthUser(
        id = "scanflow_pro_user",
        email = "user@example.com",
        displayName = "ScanFlow User",
        isGuest = false
    ),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false,
    val feedbackMessage: String? = null,
    val isNotificationsEnabled: Boolean = true,
    val storageText: String = "0 KB used",
    val currentLanguage: String = "en",
    val currentTheme: AppThemeMode = AppThemeMode.DARK,
    val totalScans: Int = 0,
    val totalQrs: Int = 0,
    val saveScanHistory: Boolean = true,
    val sendAnonymousAnalytics: Boolean = true,
    val clipboardProtection: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val historyRepository: HistoryRepository? = null,
    private val qrRepository: QrGeneratorRepository? = null
) : ViewModel() {

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _isNotificationsEnabled = MutableStateFlow(true)

    private val settingsFlow: Flow<AppSettings> = settingsRepository?.settingsFlow ?: flowOf(AppSettings())
    private val historyFlow: Flow<List<ScanHistoryItem>> = historyRepository?.getAllHistory() ?: flowOf(emptyList())
    private val qrFlow: Flow<List<UserQrCode>> = qrRepository?.getAllUserQrs() ?: flowOf(emptyList())

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.getCurrentUser(),
        syncRepository.getSyncStatus(),
        syncRepository.getLastSyncTime(),
        settingsFlow,
        _feedbackMessage,
        _isNotificationsEnabled,
        historyFlow,
        qrFlow
    ) { args: Array<Any?> ->
        val user = args[0] as AuthUser
        val syncStatus = args[1] as SyncStatus
        val lastSyncTime = args[2] as? Long
        val settings = args[3] as AppSettings
        val feedback = args[4] as? String
        val isNotif = args[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val historyList = args[6] as? List<ScanHistoryItem> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val qrList = args[7] as? List<UserQrCode> ?: emptyList()

        val displayName = if (user.isGuest && user.displayName.isNullOrEmpty()) "ScanFlow User" else (user.displayName ?: "ScanFlow User")
        val email = if (user.isGuest && user.email.isNullOrEmpty()) "user@example.com" else (user.email ?: "user@example.com")

        // Perhitungan dinamis kapasitas penyimpanan data riwayat, QR buatan pengguna, dan Room cache
        val historyBytes = historyList.sumOf { item ->
            item.content.toByteArray().size + item.title.toByteArray().size + 256L
        }
        val qrBytes = qrList.sumOf { qr ->
            qr.content.toByteArray().size + qr.title.toByteArray().size + (if (qr.logoPath != null) 32768L else 4096L) + 512L
        }
        val baseDatabaseOverhead = 65536L // 64 KB overhead skema Room DB dan metadata tabel
        val totalBytes = baseDatabaseOverhead + historyBytes + qrBytes

        val calculatedStorageText = when {
            totalBytes < 1024L -> "$totalBytes B used"
            totalBytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB used", totalBytes / 1024f)
            totalBytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f MB used", totalBytes / (1024f * 1024f))
            else -> String.format(Locale.US, "%.2f GB used", totalBytes / (1024f * 1024f * 1024f))
        }

        ProfileUiState(
            user = user.copy(displayName = displayName, email = email),
            syncStatus = syncStatus,
            lastSyncTime = lastSyncTime,
            isSyncing = syncStatus == SyncStatus.SYNCING,
            feedbackMessage = feedback,
            isNotificationsEnabled = isNotif,
            storageText = calculatedStorageText,
            currentLanguage = settings.language,
            currentTheme = settings.themeMode,
            totalScans = historyList.size,
            totalQrs = qrList.size,
            saveScanHistory = settings.saveScanHistory,
            sendAnonymousAnalytics = settings.sendAnonymousAnalytics,
            clipboardProtection = settings.clipboardProtection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            authRepository.updateProfile(name, email)
            _feedbackMessage.value = "Profil berhasil diperbarui"
        }
    }

    fun updateAvatar(uri: String?) {
        viewModelScope.launch {
            authRepository.updateAvatar(uri)
            _feedbackMessage.value = if (uri != null) "Foto profil berhasil diperbarui" else "Foto profil dihapus"
        }
    }

    fun toggleSaveScanHistory(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository?.updateSaveScanHistory(enabled)
            _feedbackMessage.value = if (enabled) "Penyimpanan riwayat diaktifkan" else "Penyimpanan riwayat dinonaktifkan"
        }
    }

    fun toggleSendAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository?.updateSendAnonymousAnalytics(enabled)
            _feedbackMessage.value = if (enabled) "Analitik anonim diaktifkan" else "Analitik anonim dinonaktifkan"
        }
    }

    fun toggleClipboardProtection(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository?.updateClipboardProtection(enabled)
            _feedbackMessage.value = if (enabled) "Proteksi clipboard diaktifkan" else "Proteksi clipboard dinonaktifkan"
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
        _feedbackMessage.value = if (enabled) "Notifikasi diaktifkan" else "Notifikasi dimatikan"
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository?.updateLanguage(lang)
            _feedbackMessage.value = if (lang == "id") "Bahasa diubah ke Bahasa Indonesia" else "Language changed to English"
        }
    }

    fun setTheme(theme: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository?.updateThemeMode(theme)
        }
    }

    fun signOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            _feedbackMessage.value = "Berhasil keluar dari akun"
            onLoggedOut()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val result = syncRepository.requestSync()
            _feedbackMessage.value = if (result.isSuccess) {
                "Sinkronisasi Berhasil: ${result.getOrNull()?.message ?: "Data tersinkron"}"
            } else {
                "Sinkronisasi Gagal: ${result.exceptionOrNull()?.message ?: "Terjadi kesalahan"}"
            }
        }
    }

    fun backupToCloud() {
        viewModelScope.launch {
            val result = syncRepository.backupToCloud()
            _feedbackMessage.value = if (result.isSuccess) {
                "Pencadangan Cloud berhasil diekspor"
            } else {
                "Pencadangan Gagal: ${result.exceptionOrNull()?.message ?: "Terjadi kesalahan"}"
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            val result = syncRepository.restoreFromCloud(null)
            _feedbackMessage.value = if (result.isSuccess) {
                val (scans, qrs) = result.getOrNull() ?: (0 to 0)
                "Pemulihan Berhasil: $scans riwayat & $qrs QR dipulihkan dari Cloud Vault"
            } else {
                "Pemulihan Gagal: ${result.exceptionOrNull()?.message ?: "Terjadi kesalahan"}"
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
