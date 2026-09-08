package com.scanflow.qr.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.scanflow.qr.feature.about.AboutScreen
import com.scanflow.qr.feature.analytics.AnalyticsScreen
import com.scanflow.qr.feature.analytics.AnalyticsViewModel
import com.scanflow.qr.feature.auth.AppLockScreen
import com.scanflow.qr.feature.auth.AuthScreen
import com.scanflow.qr.feature.auth.PinEntryScreen
import com.scanflow.qr.feature.export.DataExportScreen
import com.scanflow.qr.feature.export.DataExportViewModel
import com.scanflow.qr.feature.favorites.FavoritesScreen
import com.scanflow.qr.feature.favorites.FavoritesViewModel
import com.scanflow.qr.feature.generator.CreateQrScreen
import com.scanflow.qr.feature.generator.CreateQrViewModel
import com.scanflow.qr.feature.history.HistoryScreen
import com.scanflow.qr.feature.history.HistoryViewModel
import com.scanflow.qr.feature.home.HomeViewModel
import com.scanflow.qr.feature.main.MainScreen
import com.scanflow.qr.feature.myqr.MyQrScreen
import com.scanflow.qr.feature.myqr.MyQrViewModel
import com.scanflow.qr.feature.onboarding.OnboardingScreen
import com.scanflow.qr.feature.preview.QrPreviewScreen
import com.scanflow.qr.feature.preview.QrPreviewViewModel
import com.scanflow.qr.feature.result.ScanResultScreen
import com.scanflow.qr.feature.result.ScanResultViewModel
import com.scanflow.qr.feature.scanner.ScannerScreen
import com.scanflow.qr.feature.scanner.ScannerViewModel
import com.scanflow.qr.feature.security.SecurityScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.scanflow.qr.feature.security.SecurityViewModel
import com.scanflow.qr.feature.settings.SettingsScreen
import com.scanflow.qr.feature.settings.SettingsViewModel
import com.scanflow.qr.feature.splash.SplashScreen

@Composable
fun ScanFlowNavGraph(
    navController: NavHostController,
    isOnboardingCompleted: Boolean,
    onCompleteOnboarding: () -> Unit,
    homeViewModel: HomeViewModel? = null,
    scannerViewModel: ScannerViewModel? = null,
    scanResultViewModel: ScanResultViewModel? = null,
    createQrViewModel: CreateQrViewModel? = null,
    qrPreviewViewModel: QrPreviewViewModel? = null,
    historyViewModel: HistoryViewModel? = null,
    favoritesViewModel: FavoritesViewModel? = null,
    myQrViewModel: MyQrViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    securityViewModel: SecurityViewModel? = null,
    analyticsViewModel: AnalyticsViewModel? = null,
    dataExportViewModel: DataExportViewModel? = null,
    sharedImageUri: android.net.Uri? = null,
    onSharedUriHandled: () -> Unit = {},
    shortcutRoute: String? = null,
    onShortcutRouteHandled: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fallbackScannerViewModel = scannerViewModel ?: hiltViewModel()

    LaunchedEffect(shortcutRoute) {
        val route = shortcutRoute ?: return@LaunchedEffect
        navController.navigate(route) {
            popUpTo(Screen.Main.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onShortcutRouteHandled()
    }

    LaunchedEffect(sharedImageUri) {
        val uri = sharedImageUri ?: return@LaunchedEffect
        fallbackScannerViewModel.scanImageFromGallery(
            context = context,
            imageUri = uri,
            onNavigateResult = { scanId ->
                navController.navigate(Screen.ScanResult.createRoute(scanId))
                onSharedUriHandled()
            },
            onError = { errorMsg ->
                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                onSharedUriHandled()
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = { completed ->
                    if (completed) {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                },
                isOnboardingCompleted = isOnboardingCompleted
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    onCompleteOnboarding()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onContinueAsGuest = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onLearnPrivacy = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                homeViewModel = homeViewModel ?: hiltViewModel(),
                historyViewModel = historyViewModel ?: hiltViewModel(),
                createQrViewModel = createQrViewModel ?: hiltViewModel(),
                analyticsViewModel = analyticsViewModel ?: hiltViewModel(),
                onNavigateToScan = { navController.navigate(Screen.Scanner.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToMyQr = { navController.navigate(Screen.MyQr.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToResult = { id -> navController.navigate(Screen.ScanResult.createRoute(id)) },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                onNavigateToDataExport = { navController.navigate(Screen.DataExport.route) }
            )
        }

        composable(Screen.Scanner.route) {
            val vm: ScannerViewModel = scannerViewModel ?: hiltViewModel()
            ScannerScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateResult = { scanId ->
                    navController.navigate(Screen.ScanResult.createRoute(scanId)) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ScanResult.route,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scanId = backStackEntry.arguments?.getLong("scanId") ?: 0L
            val vm: ScanResultViewModel = scanResultViewModel ?: hiltViewModel(backStackEntry)
            ScanResultScreen(
                scanId = scanId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateQr.route) {
            val vm: CreateQrViewModel = createQrViewModel ?: hiltViewModel()
            CreateQrScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.QrPreview.route,
            arguments = listOf(navArgument("qrId") { type = NavType.LongType })
        ) { backStackEntry ->
            val qrId = backStackEntry.arguments?.getLong("qrId") ?: 0L
            val vm: QrPreviewViewModel = qrPreviewViewModel ?: hiltViewModel(backStackEntry)
            QrPreviewScreen(
                qrId = qrId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            val vm: FavoritesViewModel = favoritesViewModel ?: hiltViewModel()
            FavoritesScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = { id -> navController.navigate(Screen.ScanResult.createRoute(id)) },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) }
            )
        }

        composable(Screen.MyQr.route) {
            val vm: MyQrViewModel = myQrViewModel ?: hiltViewModel()
            MyQrScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Screen.CreateQr.route) },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) }
            )
        }

        composable(Screen.History.route) {
            val vm: HistoryViewModel = historyViewModel ?: hiltViewModel()
            HistoryScreen(
                viewModel = vm,
                onNavigateToResult = { id: Long -> navController.navigate(Screen.ScanResult.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() },
                onScanClick = { navController.navigate(Screen.Scanner.route) }
            )
        }

        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = settingsViewModel ?: hiltViewModel()
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToDataExport = { navController.navigate(Screen.DataExport.route) }
            )
        }

        composable(Screen.DataExport.route) {
            val vm: DataExportViewModel = dataExportViewModel ?: hiltViewModel()
            DataExportScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Security.route) {
            val vm: SecurityViewModel = securityViewModel ?: hiltViewModel()
            SecurityScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppLock = { navController.navigate(Screen.AppLock.route) },
                onNavigateToPinEntry = { navController.navigate(Screen.PinEntry.route) }
            )
        }

        composable(Screen.AppLock.route) {
            val vm: SettingsViewModel = settingsViewModel ?: hiltViewModel()
            val settings by vm.settings.collectAsState()
            AppLockScreen(
                settings = settings,
                onUnlockSuccess = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                onNavigateToPin = { navController.navigate(Screen.PinEntry.route) },
                isTestingMode = true
            )
        }

        composable(Screen.PinEntry.route) {
            val vm: SettingsViewModel = settingsViewModel ?: hiltViewModel()
            val settings by vm.settings.collectAsState()
            PinEntryScreen(
                settings = settings,
                onPinSuccess = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            val vm: AnalyticsViewModel = analyticsViewModel ?: hiltViewModel()
            AnalyticsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToMyQr = { navController.navigate(Screen.MyQr.route) },
                onNavigateToScan = { navController.navigate(Screen.Scanner.route) },
                onNavigateToCreate = { navController.navigate(Screen.CreateQr.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
