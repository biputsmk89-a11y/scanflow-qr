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
    val isOnboardingCompleted: Boolean = false,
    val isDynamicColorEnabled: Boolean = true,
    val autoScan: Boolean = true,
    val language: String = "en",
    val saveScanHistory: Boolean = true,
    val sendAnonymousAnalytics: Boolean = false,
    val safeUrlDetection: Boolean = true,
    val suspiciousQrWarning: Boolean = true,
    val clipboardProtection: Boolean = true,
    val lockTimeoutSeconds: Long = 0L
)

enum class LockTimeout(val seconds: Long, val labelId: String, val labelEn: String) {
    IMMEDIATELY(0L, "Segera", "Immediately"),
    THIRTY_SECONDS(30L, "30 Detik", "30 Seconds"),
    ONE_MINUTE(60L, "1 Menit", "1 Minute"),
    FIVE_MINUTES(300L, "5 Menit", "5 Minutes"),
    FIFTEEN_MINUTES(900L, "15 Menit", "15 Minutes");

    companion object {
        fun fromSeconds(seconds: Long): LockTimeout {
            return entries.find { it.seconds == seconds } ?: IMMEDIATELY
        }
    }
}

data class AuthUser(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val isGuest: Boolean = true,
    val token: String? = null,
    val lastLoginAt: Long = System.currentTimeMillis()
)

data class SyncReport(
    val success: Boolean,
    val syncedScansCount: Int = 0,
    val syncedQrsCount: Int = 0,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class SyncStatus {
    LOCAL_ONLY,
    SYNCED,
    SYNCING,
    OFFLINE,
    FAILED
}

data class DayActivity(
    val dayName: String,
    val scanCount: Int,
    val heightRatio: Float,
    val isPeak: Boolean = false
)

data class TopQrAnalyticsItem(
    val title: String,
    val type: QrType,
    val subtitle: String,
    val scanCount: Int,
    val sharePercent: Int,
    val barPercent: Int
)

data class DeviceStats(
    val iosPercent: Int = 65,
    val iosCount: Int = 835,
    val androidPercent: Int = 35,
    val androidCount: Int = 449
)

data class LocationStats(
    val rank: Int,
    val cityName: String,
    val percent: Int
)

data class HourlyStats(
    val timeWindow: String,
    val scanCount: Int,
    val percent: Int
)

data class AnalyticsSummary(
    val totalScans: Int = 0,
    val totalCreated: Int = 0,
    val totalFavorites: Int = 0,
    val scansToday: Int = 0,
    val topScanType: QrType = QrType.WEBSITE,
    val scansByType: Map<QrType, Int> = emptyMap(),
    val uniqueVisitors: Int = 0,
    val activeQrCount: Int = 0,
    val pausedQrCount: Int = 0,
    val dailyAverage: Int = 0,
    val dailyActivities: List<DayActivity> = emptyList(),
    val busiestDayText: String = "",
    val topQrs: List<TopQrAnalyticsItem> = emptyList(),
    val deviceStats: DeviceStats = DeviceStats(),
    val topLocations: List<LocationStats> = emptyList(),
    val hourlyBreakdown: List<HourlyStats> = emptyList()
)
