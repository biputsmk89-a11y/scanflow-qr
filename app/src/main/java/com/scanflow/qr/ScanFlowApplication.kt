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

    lateinit var database: ScanFlowDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set

    // Repositories
    lateinit var scanRepository: ScanRepository
        private set
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var qrGeneratorRepository: QrGeneratorRepository
        private set
    lateinit var favoriteRepository: FavoriteRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var syncRepository: SyncRepository
        private set

    // UseCases
    lateinit var parseQrCodeUseCase: ParseQrCodeUseCase
        private set
    lateinit var saveScanResultUseCase: SaveScanResultUseCase
        private set
    lateinit var generateQrCodeUseCase: GenerateQrCodeUseCase
        private set
    lateinit var getHistoryUseCase: GetHistoryUseCase
        private set
    lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
        private set
    lateinit var deleteHistoryUseCase: DeleteHistoryUseCase
        private set
    lateinit var exportQrCodeUseCase: ExportQrCodeUseCase
        private set
    lateinit var shareQrCodeUseCase: ShareQrCodeUseCase
        private set
    lateinit var assessUrlSecurityUseCase: AssessUrlSecurityUseCase
        private set
    lateinit var getSettingsUseCase: GetSettingsUseCase
        private set
    lateinit var updateSettingsUseCase: UpdateSettingsUseCase
        private set
    lateinit var getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        database = ScanFlowDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)

        scanRepository = ScanRepositoryImpl(database)
        historyRepository = HistoryRepositoryImpl(database)
        qrGeneratorRepository = QrGeneratorRepositoryImpl(this, database)
        favoriteRepository = FavoriteRepositoryImpl(database)
        settingsRepository = SettingsRepositoryImpl(preferencesManager)
        authRepository = LocalGuestAuthRepository()
        syncRepository = LocalSyncRepository()

        parseQrCodeUseCase = ParseQrCodeUseCase(scanRepository)
        saveScanResultUseCase = SaveScanResultUseCase(scanRepository)
        generateQrCodeUseCase = GenerateQrCodeUseCase(qrGeneratorRepository)
        getHistoryUseCase = GetHistoryUseCase(historyRepository)
        toggleFavoriteUseCase = ToggleFavoriteUseCase(historyRepository, qrGeneratorRepository)
        deleteHistoryUseCase = DeleteHistoryUseCase(historyRepository)
        exportQrCodeUseCase = ExportQrCodeUseCase(qrGeneratorRepository)
        shareQrCodeUseCase = ShareQrCodeUseCase(qrGeneratorRepository)
        assessUrlSecurityUseCase = AssessUrlSecurityUseCase()
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
        updateSettingsUseCase = UpdateSettingsUseCase(settingsRepository)
        getAnalyticsSummaryUseCase = GetAnalyticsSummaryUseCase(historyRepository, qrGeneratorRepository)
    }
}
