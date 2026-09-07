package com.scanflow.qr.core.di

import com.scanflow.qr.core.database.ScanFlowDatabase
import com.scanflow.qr.core.datastore.PreferencesManager
import com.scanflow.qr.domain.repository.AuthRepository
import com.scanflow.qr.domain.repository.FavoriteRepository
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.ScanRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import com.scanflow.qr.domain.repository.SyncRepository
import com.scanflow.qr.domain.usecase.AssessUrlSecurityUseCase
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.ExportQrCodeUseCase
import com.scanflow.qr.domain.usecase.GenerateQrCodeUseCase
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.ParseQrCodeUseCase
import com.scanflow.qr.domain.usecase.SaveScanResultUseCase
import com.scanflow.qr.domain.usecase.ShareQrCodeUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import com.scanflow.qr.domain.usecase.UpdateSettingsUseCase

/**
 * Dependency Injection container interface for ScanFlow QR.
 * Decouples concrete implementations from callers and enables seamless test mocking.
 */
interface AppContainer {
    val database: ScanFlowDatabase
    val preferencesManager: PreferencesManager
    val appLockManager: com.scanflow.qr.core.security.AppLockManager

    // Repositories
    val scanRepository: ScanRepository
    val historyRepository: HistoryRepository
    val qrGeneratorRepository: QrGeneratorRepository
    val favoriteRepository: FavoriteRepository
    val settingsRepository: SettingsRepository
    val authRepository: AuthRepository
    val syncRepository: SyncRepository

    // UseCases
    val parseQrCodeUseCase: ParseQrCodeUseCase
    val saveScanResultUseCase: SaveScanResultUseCase
    val generateQrCodeUseCase: GenerateQrCodeUseCase
    val getHistoryUseCase: GetHistoryUseCase
    val toggleFavoriteUseCase: ToggleFavoriteUseCase
    val deleteHistoryUseCase: DeleteHistoryUseCase
    val exportQrCodeUseCase: ExportQrCodeUseCase
    val shareQrCodeUseCase: ShareQrCodeUseCase
    val assessUrlSecurityUseCase: AssessUrlSecurityUseCase
    val getSettingsUseCase: GetSettingsUseCase
    val updateSettingsUseCase: UpdateSettingsUseCase
    val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase
}
