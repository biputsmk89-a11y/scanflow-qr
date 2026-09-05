package com.scanflow.qr.core.di

import android.content.Context
import com.scanflow.qr.core.database.ScanFlowDatabase
import com.scanflow.qr.core.datastore.PreferencesManager
import com.scanflow.qr.data.repository.FavoriteRepositoryImpl
import com.scanflow.qr.data.repository.HistoryRepositoryImpl
import com.scanflow.qr.data.repository.LocalGuestAuthRepository
import com.scanflow.qr.data.repository.LocalSyncRepository
import com.scanflow.qr.data.repository.QrGeneratorRepositoryImpl
import com.scanflow.qr.data.repository.ScanRepositoryImpl
import com.scanflow.qr.data.repository.SettingsRepositoryImpl
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
 * Production implementation of [AppContainer] providing thread-safe,
 * lazy-loaded singletons for the application.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: ScanFlowDatabase by lazy {
        ScanFlowDatabase.getInstance(context)
    }

    override val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    // Repositories
    override val scanRepository: ScanRepository by lazy {
        ScanRepositoryImpl(database)
    }

    override val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database)
    }

    override val qrGeneratorRepository: QrGeneratorRepository by lazy {
        QrGeneratorRepositoryImpl(context, database)
    }

    override val favoriteRepository: FavoriteRepository by lazy {
        FavoriteRepositoryImpl(database)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(preferencesManager)
    }

    override val authRepository: AuthRepository by lazy {
        com.scanflow.qr.data.repository.CloudAuthRepositoryImpl(preferencesManager)
    }

    override val syncRepository: SyncRepository by lazy {
        com.scanflow.qr.data.repository.CloudSyncRepositoryImpl(context, database, preferencesManager)
    }

    // UseCases
    override val parseQrCodeUseCase: ParseQrCodeUseCase by lazy {
        ParseQrCodeUseCase(scanRepository)
    }

    override val saveScanResultUseCase: SaveScanResultUseCase by lazy {
        SaveScanResultUseCase(scanRepository)
    }

    override val generateQrCodeUseCase: GenerateQrCodeUseCase by lazy {
        GenerateQrCodeUseCase(qrGeneratorRepository)
    }

    override val getHistoryUseCase: GetHistoryUseCase by lazy {
        GetHistoryUseCase(historyRepository)
    }

    override val toggleFavoriteUseCase: ToggleFavoriteUseCase by lazy {
        ToggleFavoriteUseCase(historyRepository, qrGeneratorRepository)
    }

    override val deleteHistoryUseCase: DeleteHistoryUseCase by lazy {
        DeleteHistoryUseCase(historyRepository)
    }

    override val exportQrCodeUseCase: ExportQrCodeUseCase by lazy {
        ExportQrCodeUseCase(qrGeneratorRepository)
    }

    override val shareQrCodeUseCase: ShareQrCodeUseCase by lazy {
        ShareQrCodeUseCase(qrGeneratorRepository)
    }

    override val assessUrlSecurityUseCase: AssessUrlSecurityUseCase by lazy {
        AssessUrlSecurityUseCase()
    }

    override val getSettingsUseCase: GetSettingsUseCase by lazy {
        GetSettingsUseCase(settingsRepository)
    }

    override val updateSettingsUseCase: UpdateSettingsUseCase by lazy {
        UpdateSettingsUseCase(settingsRepository)
    }

    override val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase by lazy {
        GetAnalyticsSummaryUseCase(historyRepository, qrGeneratorRepository)
    }
}
