package com.scanflow.qr

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.scanflow.qr.core.designsystem.ScanFlowQRTheme
import com.scanflow.qr.core.navigation.ScanFlowNavGraph
import com.scanflow.qr.core.security.BiometricAuthManager
import com.scanflow.qr.domain.model.AppThemeMode
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
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ScanFlowApplication

        setContent {
            val settings by app.settingsRepository.settingsFlow.collectAsState(
                initial = com.scanflow.qr.domain.model.AppSettings()
            )

            val isDarkTheme = when (settings.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var isUnlocked by remember { mutableStateOf(!settings.isAppLockEnabled) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(settings.isAppLockEnabled) {
                if (settings.isAppLockEnabled && !isUnlocked) {
                    if (settings.isBiometricEnabled && BiometricAuthManager.isBiometricAvailable(this@MainActivity)) {
                        BiometricAuthManager.authenticate(
                            activity = this@MainActivity,
                            onSuccess = { isUnlocked = true },
                            onError = { /* fallback to unlocked if failed or prompt again */ }
                        )
                    } else {
                        isUnlocked = true // PIN / Guest fallback
                    }
                } else {
                    isUnlocked = true
                }
            }

            ScanFlowQRTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        val navController = rememberNavController()

                        val homeViewModel = remember {
                            HomeViewModel(
                                app.getHistoryUseCase,
                                app.getAnalyticsSummaryUseCase,
                                app.toggleFavoriteUseCase
                            )
                        }

                        val scannerViewModel = remember {
                            ScannerViewModel(
                                app.parseQrCodeUseCase,
                                app.saveScanResultUseCase,
                                app.getSettingsUseCase
                            )
                        }

                        val scanResultViewModel = remember {
                            ScanResultViewModel(
                                app.historyRepository,
                                app.toggleFavoriteUseCase
                            )
                        }

                        val createQrViewModel = remember {
                            CreateQrViewModel(app.qrGeneratorRepository)
                        }

                        val qrPreviewViewModel = remember {
                            QrPreviewViewModel(app.qrGeneratorRepository)
                        }

                        val historyViewModel = remember {
                            HistoryViewModel(
                                app.historyRepository,
                                app.getHistoryUseCase,
                                app.toggleFavoriteUseCase,
                                app.deleteHistoryUseCase
                            )
                        }

                        val favoritesViewModel = remember {
                            FavoritesViewModel(
                                app.favoriteRepository,
                                app.toggleFavoriteUseCase
                            )
                        }

                        val myQrViewModel = remember {
                            MyQrViewModel(app.qrGeneratorRepository)
                        }

                        val settingsViewModel = remember {
                            SettingsViewModel(
                                app.getSettingsUseCase,
                                app.updateSettingsUseCase
                            )
                        }

                        val securityViewModel = remember {
                            SecurityViewModel(
                                app.getSettingsUseCase,
                                app.updateSettingsUseCase
                            )
                        }

                        val analyticsViewModel = remember {
                            AnalyticsViewModel(app.getAnalyticsSummaryUseCase)
                        }

                        ScanFlowNavGraph(
                            navController = navController,
                            homeViewModel = homeViewModel,
                            scannerViewModel = scannerViewModel,
                            scanResultViewModel = scanResultViewModel,
                            createQrViewModel = createQrViewModel,
                            qrPreviewViewModel = qrPreviewViewModel,
                            historyViewModel = historyViewModel,
                            favoritesViewModel = favoritesViewModel,
                            myQrViewModel = myQrViewModel,
                            settingsViewModel = settingsViewModel,
                            securityViewModel = securityViewModel,
                            analyticsViewModel = analyticsViewModel,
                            isOnboardingCompleted = settings.isOnboardingCompleted,
                            onCompleteOnboarding = {
                                scope.launch {
                                    app.updateSettingsUseCase.completeOnboarding()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
