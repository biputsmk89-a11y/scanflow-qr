package com.scanflow.qr.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.qr.core.database.ScanFlowDatabase
import com.scanflow.qr.core.datastore.PreferencesManager
import com.scanflow.qr.core.utils.ImageExporter
import com.scanflow.qr.core.utils.QrCodeGenerator
import com.scanflow.qr.data.mapper.toDomain
import com.scanflow.qr.data.mapper.toEntity
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.model.AuthUser
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.SyncStatus
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.AuthRepository
import com.scanflow.qr.domain.repository.FavoriteRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import com.scanflow.qr.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class QrGeneratorRepositoryImpl(
    private val context: Context,
    private val database: ScanFlowDatabase
) : QrGeneratorRepository {

    override fun generateQrBitmap(content: String, config: QrStyleConfig): Bitmap? {
        return QrCodeGenerator.generateQrBitmap(content, config)
    }

    override suspend fun saveUserQr(userQrCode: UserQrCode): Long {
        return database.qrCodeDao().insertQr(userQrCode.toEntity())
    }

    override fun getAllUserQrs(): Flow<List<UserQrCode>> {
        return database.qrCodeDao().getAllUserQrs().map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteUserQrs(): Flow<List<UserQrCode>> {
        return database.qrCodeDao().getFavoriteUserQrs().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getUserQrById(id: Long): UserQrCode? {
        return database.qrCodeDao().getQrById(id)?.toDomain()
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        database.qrCodeDao().updateFavoriteStatus(id, isFavorite)
    }

    override suspend fun incrementScanCount(id: Long) {
        database.qrCodeDao().incrementScanCount(id)
    }

    override suspend fun incrementShareCount(id: Long) {
        database.qrCodeDao().incrementShareCount(id)
    }

    override suspend fun incrementDownloadCount(id: Long) {
        database.qrCodeDao().incrementDownloadCount(id)
    }

    override suspend fun deleteUserQr(id: Long) {
        database.qrCodeDao().deleteQrById(id)
    }

    override suspend fun exportQrToGallery(bitmap: Bitmap, title: String): Uri? {
        return ImageExporter.saveBitmapToGallery(context, bitmap, title)
    }

    override suspend fun cacheQrForSharing(bitmap: Bitmap, filename: String): Uri? {
        return ImageExporter.saveBitmapToCache(context, bitmap, filename)
    }

    override fun generateQrSvg(content: String, config: QrStyleConfig): String? {
        return QrCodeGenerator.generateQrSvg(content, config)
    }

    override suspend fun exportQrSvg(svgContent: String, title: String): Uri? {
        return ImageExporter.saveSvgToStorage(context, svgContent, title)
    }

    override suspend fun cacheQrSvgForSharing(svgContent: String, filename: String): Uri? {
        return ImageExporter.saveSvgToCache(context, svgContent, filename)
    }

    override suspend fun exportQrPdf(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri? {
        val pdf = com.scanflow.qr.core.utils.PdfDocumentExporter.createPdfDocument(title, type, content, bitmap)
        return com.scanflow.qr.core.utils.PdfDocumentExporter.savePdfToStorage(context, pdf, filename)
    }

    override suspend fun cacheQrPdfForSharing(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri? {
        val pdf = com.scanflow.qr.core.utils.PdfDocumentExporter.createPdfDocument(title, type, content, bitmap)
        return com.scanflow.qr.core.utils.PdfDocumentExporter.savePdfToCache(context, pdf, filename)
    }
}

class FavoriteRepositoryImpl(
    private val database: ScanFlowDatabase
) : FavoriteRepository {
    override fun getAllFavoriteScans(): Flow<List<ScanHistoryItem>> {
        return database.scanHistoryDao().getFavoriteScans().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllFavoriteCreatedQrs(): Flow<List<UserQrCode>> {
        return database.qrCodeDao().getFavoriteUserQrs().map { list -> list.map { it.toDomain() } }
    }
}

class SettingsRepositoryImpl(
    private val preferencesManager: PreferencesManager
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = preferencesManager.settingsFlow

    override suspend fun updateThemeMode(themeMode: AppThemeMode) {
        preferencesManager.updateThemeMode(themeMode)
    }

    override suspend fun updateVibrate(enabled: Boolean) {
        preferencesManager.updateVibrate(enabled)
    }

    override suspend fun updateBeep(enabled: Boolean) {
        preferencesManager.updateBeep(enabled)
    }

    override suspend fun updateAutoOpen(enabled: Boolean) {
        preferencesManager.updateAutoOpen(enabled)
    }

    override suspend fun updateAutoCopy(enabled: Boolean) {
        preferencesManager.updateAutoCopy(enabled)
    }

    override suspend fun updateAppLock(enabled: Boolean) {
        preferencesManager.updateAppLock(enabled)
    }

    override suspend fun updateBiometric(enabled: Boolean) {
        preferencesManager.updateBiometric(enabled)
    }

    override suspend fun updatePinCode(pin: String?) {
        preferencesManager.updatePinCode(pin)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }
}

class LocalGuestAuthRepository : AuthRepository {
    private val guestUser = AuthUser(
        id = "local_guest_user",
        email = null,
        displayName = "Guest User",
        isGuest = true
    )

    override fun getCurrentUser(): Flow<AuthUser> = flowOf(guestUser)

    override suspend fun signIn(email: String, password: String): Result<AuthUser> {
        return Result.success(guestUser.copy(email = email, displayName = email.substringBefore("@")))
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<AuthUser> {
        return Result.success(guestUser.copy(email = email, displayName = displayName))
    }

    override suspend fun signInAsGuest(): AuthUser = guestUser

    override suspend fun signOut() {}
}

class LocalSyncRepository : SyncRepository {
    override fun getSyncStatus(): Flow<SyncStatus> = flowOf(SyncStatus.LOCAL_ONLY)

    override fun getLastSyncTime(): Flow<Long?> = flowOf(null)

    override suspend fun requestSync(): Result<com.scanflow.qr.domain.model.SyncReport> {
        return Result.success(com.scanflow.qr.domain.model.SyncReport(success = true, message = "Local Mode"))
    }

    override suspend fun backupToCloud(): Result<String> {
        return Result.success("local")
    }

    override suspend fun restoreFromCloud(uri: Uri?): Result<Pair<Int, Int>> {
        return Result.success(0 to 0)
    }
}
