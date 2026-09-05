package com.scanflow.qr.core.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scanflow.qr.ScanFlowApplication
import com.scanflow.qr.feature.analytics.AnalyticsViewModel
import com.scanflow.qr.feature.favorites.FavoritesViewModel
import com.scanflow.qr.feature.generator.CreateQrViewModel
import com.scanflow.qr.feature.history.HistoryViewModel
import com.scanflow.qr.feature.home.HomeViewModel
import com.scanflow.qr.feature.myqr.MyQrViewModel
import com.scanflow.qr.feature.preview.QrPreviewViewModel
import com.scanflow.qr.feature.result.ScanResultViewModel
import com.scanflow.qr.feature.scanner.ScannerViewModel
import com.scanflow.qr.feature.security.SecurityViewModel
import com.scanflow.qr.feature.settings.SettingsViewModel

/**
 * Centralized ViewModel Factory provider that maps ViewModels to their required dependencies
 * obtained from the application's [AppContainer].
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = scanFlowApplication().container
            HomeViewModel(
                getHistoryUseCase = container.getHistoryUseCase,
                getAnalyticsSummaryUseCase = container.getAnalyticsSummaryUseCase,
                toggleFavoriteUseCase = container.toggleFavoriteUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            ScannerViewModel(
                parseQrCodeUseCase = container.parseQrCodeUseCase,
                saveScanResultUseCase = container.saveScanResultUseCase,
                getSettingsUseCase = container.getSettingsUseCase,
                deleteHistoryUseCase = container.deleteHistoryUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            ScanResultViewModel(
                historyRepository = container.historyRepository,
                toggleFavoriteUseCase = container.toggleFavoriteUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            CreateQrViewModel(
                qrRepository = container.qrGeneratorRepository
            )
        }
        initializer {
            val container = scanFlowApplication().container
            QrPreviewViewModel(
                qrRepository = container.qrGeneratorRepository
            )
        }
        initializer {
            val container = scanFlowApplication().container
            HistoryViewModel(
                historyRepository = container.historyRepository,
                getHistoryUseCase = container.getHistoryUseCase,
                toggleFavoriteUseCase = container.toggleFavoriteUseCase,
                deleteHistoryUseCase = container.deleteHistoryUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            FavoritesViewModel(
                favoriteRepository = container.favoriteRepository,
                toggleFavoriteUseCase = container.toggleFavoriteUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            MyQrViewModel(
                qrRepository = container.qrGeneratorRepository
            )
        }
        initializer {
            val container = scanFlowApplication().container
            SettingsViewModel(
                getSettingsUseCase = container.getSettingsUseCase,
                updateSettingsUseCase = container.updateSettingsUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            SecurityViewModel(
                getSettingsUseCase = container.getSettingsUseCase,
                updateSettingsUseCase = container.updateSettingsUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            AnalyticsViewModel(
                getAnalyticsSummaryUseCase = container.getAnalyticsSummaryUseCase
            )
        }
        initializer {
            val container = scanFlowApplication().container
            com.scanflow.qr.feature.profile.ProfileViewModel(
                authRepository = container.authRepository,
                syncRepository = container.syncRepository
            )
        }
        initializer {
            val container = scanFlowApplication().container
            com.scanflow.qr.feature.auth.AuthViewModel(
                authRepository = container.authRepository
            )
        }
    }
}

/**
 * Extension function to retrieve [ScanFlowApplication] from [CreationExtras].
 */
fun CreationExtras.scanFlowApplication(): ScanFlowApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ScanFlowApplication)
