package com.scanflow.qr.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.model.AuthUser
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.SyncStatus
import com.scanflow.qr.domain.model.UserQrCode
import kotlinx.coroutines.flow.Flow

interface QrGeneratorRepository {
    fun generateQrBitmap(content: String, config: QrStyleConfig): Bitmap?
    suspend fun saveUserQr(userQrCode: UserQrCode): Long
    suspend fun updateUserQr(userQrCode: UserQrCode)
    suspend fun updateUserQrStyle(id: Long, config: QrStyleConfig)
    fun getAllUserQrs(): Flow<List<UserQrCode>>
    fun getFavoriteUserQrs(): Flow<List<UserQrCode>>
    suspend fun getUserQrById(id: Long): UserQrCode?
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun incrementScanCount(id: Long)
    suspend fun incrementShareCount(id: Long)
    suspend fun incrementDownloadCount(id: Long)
    suspend fun deleteUserQr(id: Long)
    suspend fun exportQrToGallery(bitmap: Bitmap, title: String): Uri?
    suspend fun cacheQrForSharing(bitmap: Bitmap, filename: String): Uri?
    fun generateQrSvg(content: String, config: QrStyleConfig): String?
    fun generateBarcodeBitmap(
        content: String,
        formatName: String = "CODE_128",
        width: Int = 800,
        height: Int = 300,
        foregroundColor: Int = 0xFF000000.toInt(),
        backgroundColor: Int = 0xFFFFFFFF.toInt()
    ): Bitmap? = null
    fun generateBarcodeSvg(
        content: String,
        formatName: String = "CODE_128",
        foregroundColor: Int = 0xFF000000.toInt(),
        backgroundColor: Int = 0xFFFFFFFF.toInt()
    ): String? = null
    suspend fun exportQrSvg(svgContent: String, title: String): Uri?
    suspend fun cacheQrSvgForSharing(svgContent: String, filename: String): Uri?
    suspend fun exportQrPdf(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri?
    suspend fun cacheQrPdfForSharing(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri?
}

interface FavoriteRepository {
    fun getAllFavoriteScans(): Flow<List<com.scanflow.qr.domain.model.ScanHistoryItem>>
    fun getAllFavoriteCreatedQrs(): Flow<List<UserQrCode>>
}

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun updateThemeMode(themeMode: AppThemeMode)
    suspend fun updateVibrate(enabled: Boolean)
    suspend fun updateBeep(enabled: Boolean)
    suspend fun updateAutoOpen(enabled: Boolean)
    suspend fun updateAutoCopy(enabled: Boolean)
    suspend fun updateAppLock(enabled: Boolean)
    suspend fun updateBiometric(enabled: Boolean)
    suspend fun updateLockTimeout(seconds: Long)
    suspend fun updatePinCode(pin: String?)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updateDynamicColor(enabled: Boolean)
    suspend fun updateAutoScan(enabled: Boolean)
    suspend fun updateLanguage(lang: String)
    suspend fun updateSaveScanHistory(enabled: Boolean)
    suspend fun updateSendAnonymousAnalytics(enabled: Boolean)
    suspend fun updateSafeUrlDetection(enabled: Boolean)
    suspend fun updateSuspiciousQrWarning(enabled: Boolean)
    suspend fun updateClipboardProtection(enabled: Boolean)
}

interface AuthRepository {
    fun getCurrentUser(): Flow<AuthUser>
    suspend fun signIn(email: String, password: String): Result<AuthUser>
    suspend fun signUp(email: String, password: String, displayName: String): Result<AuthUser>
    suspend fun signInAsGuest(): AuthUser
    suspend fun signOut()
    suspend fun updateProfile(name: String, email: String): Result<AuthUser>
    suspend fun updateAvatar(avatarUri: String?): Result<Unit>
}

interface SyncRepository {
    fun getSyncStatus(): Flow<SyncStatus>
    fun getLastSyncTime(): Flow<Long?>
    suspend fun requestSync(): Result<com.scanflow.qr.domain.model.SyncReport>
    suspend fun backupToCloud(): Result<String>
    suspend fun restoreFromCloud(uri: Uri? = null): Result<Pair<Int, Int>>
}
