package com.scanflow.qr.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.scanflow.qr.feature.about.AboutScreen
import com.scanflow.qr.feature.analytics.AnalyticsScreen
import com.scanflow.qr.feature.analytics.AnalyticsViewModel
import com.scanflow.qr.feature.auth.AuthScreen
import com.scanflow.qr.feature.favorites.FavoritesScreen
import com.scanflow.qr.feature.favorites.FavoritesViewModel
import com.scanflow.qr.feature.generator.CreateQrScreen
import com.scanflow.qr.feature.generator.CreateQrViewModel
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
    homeViewModel: HomeViewModel = hiltViewModel(),
    scannerViewModel: ScannerViewModel = hiltViewModel(),
    scanResultViewModel: ScanResultViewModel = hiltViewModel(),
    createQrViewModel: CreateQrViewModel = hiltViewModel(),
    qrPreviewViewModel: QrPreviewViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    myQrViewModel: MyQrViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel(),
    analyticsViewModel: AnalyticsViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
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
                homeViewModel = homeViewModel,
                historyViewModel = historyViewModel,
                createQrViewModel = createQrViewModel,
                onNavigateToScan = { navController.navigate(Screen.Scanner.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToMyQr = { navController.navigate(Screen.MyQr.route) },
                onNavigateToResult = { id -> navController.navigate(Screen.ScanResult.createRoute(id)) },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                viewModel = scannerViewModel,
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
            ScanResultScreen(
                scanId = scanId,
                viewModel = scanResultViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateQr.route) {
            CreateQrScreen(
                viewModel = createQrViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) }
            )
        }

        composable(
            route = Screen.QrPreview.route,
            arguments = listOf(navArgument("qrId") { type = NavType.LongType })
        ) { backStackEntry ->
            val qrId = backStackEntry.arguments?.getLong("qrId") ?: 0L
            QrPreviewScreen(
                qrId = qrId,
                viewModel = qrPreviewViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                viewModel = favoritesViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = { id -> navController.navigate(Screen.ScanResult.createRoute(id)) },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) }
            )
        }

        composable(Screen.MyQr.route) {
            MyQrScreen(
                viewModel = myQrViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Screen.CreateQr.route) },
                onNavigateToPreview = { id -> navController.navigate(Screen.QrPreview.createRoute(id)) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.Security.route) {
            SecurityScreen(
                viewModel = securityViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
