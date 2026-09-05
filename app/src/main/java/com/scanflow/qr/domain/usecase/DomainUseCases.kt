package com.scanflow.qr.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.qr.core.security.UrlSecurityChecker
import com.scanflow.qr.domain.model.AnalyticsSummary
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.SecurityAssessment
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.FavoriteRepository
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.ScanRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ParseQrCodeUseCase(private val repository: ScanRepository) {
    operator fun invoke(rawContent: String, format: String = "QR_CODE"): QrCodeData {
        return repository.parseScannedContent(rawContent, format)
    }
}

class SaveScanResultUseCase(private val repository: ScanRepository) {
    suspend operator fun invoke(qrCodeData: QrCodeData): Long {
        return repository.saveScanResult(qrCodeData)
    }
}

class GenerateQrCodeUseCase(private val repository: QrGeneratorRepository) {
    operator fun invoke(content: String, config: QrStyleConfig): Bitmap? {
        return repository.generateQrBitmap(content, config)
    }
}

class GetHistoryUseCase(private val repository: HistoryRepository) {
    operator fun invoke(): Flow<List<ScanHistoryItem>> = repository.getAllHistory()
    fun getRecent(limit: Int): Flow<List<ScanHistoryItem>> = repository.getRecentHistory(limit)
    fun getByType(type: QrType): Flow<List<ScanHistoryItem>> = repository.getHistoryByType(type)
    fun search(query: String): Flow<List<ScanHistoryItem>> = repository.searchHistory(query)
}

class ToggleFavoriteUseCase(
    private val historyRepository: HistoryRepository,
    private val qrRepository: QrGeneratorRepository
) {
    suspend fun toggleScanFavorite(id: Long, isFavorite: Boolean) {
        historyRepository.toggleFavorite(id, isFavorite)
    }

    suspend fun toggleUserQrFavorite(id: Long, isFavorite: Boolean) {
        qrRepository.toggleFavorite(id, isFavorite)
    }
}

class DeleteHistoryUseCase(private val repository: HistoryRepository) {
    suspend fun deleteItem(id: Long) = repository.deleteHistoryItem(id)
    suspend fun deleteItems(ids: List<Long>) = repository.deleteHistoryItems(ids)
    suspend fun clearAll() = repository.clearAllHistory()
}

class ExportQrCodeUseCase(private val repository: QrGeneratorRepository) {
    suspend operator fun invoke(bitmap: Bitmap, title: String): Uri? {
        return repository.exportQrToGallery(bitmap, title)
    }
}

class ShareQrCodeUseCase(private val repository: QrGeneratorRepository) {
    suspend operator fun invoke(bitmap: Bitmap, filename: String): Uri? {
        return repository.cacheQrForSharing(bitmap, filename)
    }
}

class AssessUrlSecurityUseCase {
    operator fun invoke(url: String): SecurityAssessment {
        return UrlSecurityChecker.assessUrl(url)
    }
}

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.settingsFlow
}

class UpdateSettingsUseCase(private val repository: SettingsRepository) {
    suspend fun updateTheme(mode: AppThemeMode) = repository.updateThemeMode(mode)
    suspend fun updateVibrate(enabled: Boolean) = repository.updateVibrate(enabled)
    suspend fun updateBeep(enabled: Boolean) = repository.updateBeep(enabled)
    suspend fun updateAutoOpen(enabled: Boolean) = repository.updateAutoOpen(enabled)
    suspend fun updateAutoCopy(enabled: Boolean) = repository.updateAutoCopy(enabled)
    suspend fun updateAppLock(enabled: Boolean) = repository.updateAppLock(enabled)
    suspend fun updateBiometric(enabled: Boolean) = repository.updateBiometric(enabled)
    suspend fun updatePin(pin: String?) = repository.updatePinCode(pin)
    suspend fun completeOnboarding() = repository.setOnboardingCompleted(true)
    suspend fun updateDynamicColor(enabled: Boolean) = repository.updateDynamicColor(enabled)
}

class GetAnalyticsSummaryUseCase(
    private val historyRepository: HistoryRepository,
    private val qrRepository: QrGeneratorRepository
) {
    operator fun invoke(): Flow<AnalyticsSummary> {
        return combine(
            historyRepository.getAllHistory(),
            historyRepository.getScansTodayCount(),
            qrRepository.getAllUserQrs()
        ) { historyList, scansToday, userQrs ->
            val scansByType = historyList.groupBy { it.type }.mapValues { it.value.size }
            val topType = scansByType.maxByOrNull { it.value }?.key ?: QrType.WEBSITE
            val totalFavorites = historyList.count { it.isFavorite } + userQrs.count { it.isFavorite }

            AnalyticsSummary(
                totalScans = historyList.size,
                totalCreated = userQrs.size,
                totalFavorites = totalFavorites,
                scansToday = scansToday,
                topScanType = topType,
                scansByType = scansByType
            )
        }
    }
}
