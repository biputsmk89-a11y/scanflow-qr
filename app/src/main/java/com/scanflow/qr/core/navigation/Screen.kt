package com.scanflow.qr.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.scanflow.qr.R

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Main : Screen("main")
    data object Scanner : Screen("scanner")
    data object ScanResult : Screen("scan_result/{scanId}") {
        fun createRoute(scanId: Long) = "scan_result/$scanId"
    }
    data object CreateQr : Screen("create_qr")
    data object QrPreview : Screen("qr_preview/{qrId}") {
        fun createRoute(qrId: Long) = "qr_preview/$qrId"
    }
    data object Favorites : Screen("favorites")
    data object MyQr : Screen("my_qr")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Security : Screen("security")
    data object Analytics : Screen("analytics")
    data object AppLock : Screen("app_lock")
    data object PinEntry : Screen("pin_entry")
    data object DataExport : Screen("data_export")
    data object About : Screen("about")
}

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    @Deprecated("Use titleRes with stringResource for dynamic localization")
    val title: String = ""
) {
    data object Home : BottomNavItem("home_tab", R.string.nav_home, Icons.Default.Home)
    data object Scan : BottomNavItem("scan_tab", R.string.nav_scan, Icons.Default.QrCodeScanner)
    data object Create : BottomNavItem("create_tab", R.string.nav_create, Icons.Default.AddCircle)
    data object Analytics : BottomNavItem("analytics_tab", R.string.nav_analytics, Icons.Default.Insights)
    data object Profile : BottomNavItem("profile_tab", R.string.nav_profile, Icons.Default.Person)
    data object History : BottomNavItem("history_tab", R.string.nav_history, Icons.Default.History)

    companion object {
        val items: List<BottomNavItem>
            get() = listOf(Home, Create, Scan, Analytics, Profile)
    }
}
