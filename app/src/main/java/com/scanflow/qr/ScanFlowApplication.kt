package com.scanflow.qr

import android.app.Application
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

class ScanFlowApplication : Application() {

    val database: ScanFlowDatabase by lazy {
        ScanFlowDatabase.getInstance(this)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(this)
    }

    // Repositories
    val scanRepository: ScanRepository by lazy {
        ScanRepositoryImpl(database)
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database)
    }

    val qrGeneratorRepository: QrGeneratorRepository by lazy {
        QrGeneratorRepositoryImpl(this, database)
    }

    val favoriteRepository: FavoriteRepository by lazy {
        FavoriteRepositoryImpl(database)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(preferencesManager)
    }

    val authRepository: AuthRepository by lazy {
        LocalGuestAuthRepository()
    }

    val syncRepository: SyncRepository by lazy {
        LocalSyncRepository()
    }

    // UseCases
    val parseQrCodeUseCase: ParseQrCodeUseCase by lazy {
        ParseQrCodeUseCase(scanRepository)
    }

    val saveScanResultUseCase: SaveScanResultUseCase by lazy {
        SaveScanResultUseCase(scanRepository)
    }

    val generateQrCodeUseCase: GenerateQrCodeUseCase by lazy {
        GenerateQrCodeUseCase(qrGeneratorRepository)
    }

    val getHistoryUseCase: GetHistoryUseCase by lazy {
        GetHistoryUseCase(historyRepository)
    }

    val toggleFavoriteUseCase: ToggleFavoriteUseCase by lazy {
        ToggleFavoriteUseCase(historyRepository, qrGeneratorRepository)
    }

    val deleteHistoryUseCase: DeleteHistoryUseCase by lazy {
        DeleteHistoryUseCase(historyRepository)
    }

    val exportQrCodeUseCase: ExportQrCodeUseCase by lazy {
        ExportQrCodeUseCase(qrGeneratorRepository)
    }

    val shareQrCodeUseCase: ShareQrCodeUseCase by lazy {
        ShareQrCodeUseCase(qrGeneratorRepository)
    }

    val assessUrlSecurityUseCase: AssessUrlSecurityUseCase by lazy {
        AssessUrlSecurityUseCase()
    }

    val getSettingsUseCase: GetSettingsUseCase by lazy {
        GetSettingsUseCase(settingsRepository)
    }

    val updateSettingsUseCase: UpdateSettingsUseCase by lazy {
        UpdateSettingsUseCase(settingsRepository)
    }

    val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase by lazy {
        GetAnalyticsSummaryUseCase(historyRepository, qrGeneratorRepository)
    }
}
