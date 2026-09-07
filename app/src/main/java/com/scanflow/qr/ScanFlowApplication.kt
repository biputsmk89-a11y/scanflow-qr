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

import com.scanflow.qr.core.crash.CrashRecoveryHandler
import com.scanflow.qr.core.di.AppContainer
import com.scanflow.qr.core.di.DefaultAppContainer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScanFlowApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        CrashRecoveryHandler.install(this)
        container = DefaultAppContainer(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            com.scanflow.qr.core.utils.SoundHelper.release()
        }
    }


    val database: ScanFlowDatabase get() = container.database
    val preferencesManager: PreferencesManager get() = container.preferencesManager

    // Repositories
    val scanRepository: ScanRepository get() = container.scanRepository
    val historyRepository: HistoryRepository get() = container.historyRepository
    val qrGeneratorRepository: QrGeneratorRepository get() = container.qrGeneratorRepository
    val favoriteRepository: FavoriteRepository get() = container.favoriteRepository
    val settingsRepository: SettingsRepository get() = container.settingsRepository
    val authRepository: AuthRepository get() = container.authRepository
    val syncRepository: SyncRepository get() = container.syncRepository
    val appLockManager: com.scanflow.qr.core.security.AppLockManager get() = container.appLockManager

    // UseCases
    val parseQrCodeUseCase: ParseQrCodeUseCase get() = container.parseQrCodeUseCase
    val saveScanResultUseCase: SaveScanResultUseCase get() = container.saveScanResultUseCase
    val generateQrCodeUseCase: GenerateQrCodeUseCase get() = container.generateQrCodeUseCase
    val getHistoryUseCase: GetHistoryUseCase get() = container.getHistoryUseCase
    val toggleFavoriteUseCase: ToggleFavoriteUseCase get() = container.toggleFavoriteUseCase
    val deleteHistoryUseCase: DeleteHistoryUseCase get() = container.deleteHistoryUseCase
    val exportQrCodeUseCase: ExportQrCodeUseCase get() = container.exportQrCodeUseCase
    val shareQrCodeUseCase: ShareQrCodeUseCase get() = container.shareQrCodeUseCase
    val assessUrlSecurityUseCase: AssessUrlSecurityUseCase get() = container.assessUrlSecurityUseCase
    val getSettingsUseCase: GetSettingsUseCase get() = container.getSettingsUseCase
    val updateSettingsUseCase: UpdateSettingsUseCase get() = container.updateSettingsUseCase
    val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase get() = container.getAnalyticsSummaryUseCase
}
