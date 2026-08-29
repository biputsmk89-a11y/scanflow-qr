package com.scanflow.qr.domain.model

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val vibrateOnScan: Boolean = true,
    val beepOnScan: Boolean = true,
    val autoOpenUrl: Boolean = false,
    val autoCopyToClipboard: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val pinCode: String? = null,
    val isOnboardingCompleted: Boolean = false
)

data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val isGuest: Boolean = true
)

enum class SyncStatus {
    LOCAL_ONLY,
    SYNCED,
    SYNCING,
    OFFLINE,
    FAILED
}

data class AnalyticsSummary(
    val totalScans: Int = 0,
    val totalCreated: Int = 0,
    val totalFavorites: Int = 0,
    val scansToday: Int = 0,
    val topScanType: QrType = QrType.WEBSITE,
    val scansByType: Map<QrType, Int> = emptyMap()
)
