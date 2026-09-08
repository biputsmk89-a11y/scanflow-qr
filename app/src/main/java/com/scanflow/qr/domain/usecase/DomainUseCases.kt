package com.scanflow.qr.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.qr.core.security.UrlSecurityChecker
import com.scanflow.qr.domain.model.AnalyticsSummary
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.SecurityAssessment
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.FavoriteRepository
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.ScanRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ParseQrCodeUseCase(private val repository: ScanRepository) {
    operator fun invoke(rawContent: String, format: String = "QR_CODE"): QrCodeData {
        return repository.parseScannedContent(rawContent, format)
    }
}

class SaveScanResultUseCase(private val repository: ScanRepository) {
    suspend operator fun invoke(qrCodeData: QrCodeData): Long {
        return repository.saveScanResult(qrCodeData)
    }
}

class GenerateQrCodeUseCase(private val repository: QrGeneratorRepository) {
    operator fun invoke(content: String, config: QrStyleConfig): Bitmap? {
        return repository.generateQrBitmap(content, config)
    }
}

class GetHistoryUseCase(private val repository: HistoryRepository) {
    operator fun invoke(): Flow<List<ScanHistoryItem>> = repository.getAllHistory()
    fun getRecent(limit: Int): Flow<List<ScanHistoryItem>> = repository.getRecentHistory(limit)
    fun getByType(type: QrType): Flow<List<ScanHistoryItem>> = repository.getHistoryByType(type)
    fun search(query: String, type: QrType? = null): Flow<List<ScanHistoryItem>> {
        val searchFlow = repository.searchHistory(query)
        return if (type != null) {
            searchFlow.map { list -> list.filter { it.type == type } }
        } else {
            searchFlow
        }
    }
}

class ToggleFavoriteUseCase(
    private val historyRepository: HistoryRepository,
    private val qrRepository: QrGeneratorRepository
) {
    suspend fun toggleScanFavorite(id: Long, isFavorite: Boolean) {
        historyRepository.toggleFavorite(id, isFavorite)
    }

    suspend fun toggleUserQrFavorite(id: Long, isFavorite: Boolean) {
        qrRepository.toggleFavorite(id, isFavorite)
    }
}

class DeleteHistoryUseCase(private val repository: HistoryRepository) {
    suspend fun deleteItem(id: Long) = repository.deleteHistoryItem(id)
    suspend fun deleteItems(ids: List<Long>) = repository.deleteHistoryItems(ids)
    suspend fun clearAll() = repository.clearAllHistory()
}

class ExportQrCodeUseCase(private val repository: QrGeneratorRepository) {
    suspend operator fun invoke(bitmap: Bitmap, title: String): Uri? {
        return repository.exportQrToGallery(bitmap, title)
    }
}

class ShareQrCodeUseCase(private val repository: QrGeneratorRepository) {
    suspend operator fun invoke(bitmap: Bitmap, filename: String): Uri? {
        return repository.cacheQrForSharing(bitmap, filename)
    }
}

class AssessUrlSecurityUseCase {
    operator fun invoke(url: String): SecurityAssessment {
        return UrlSecurityChecker.assessUrl(url)
    }
}

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.settingsFlow
}

class UpdateSettingsUseCase(private val repository: SettingsRepository) {
    suspend fun updateTheme(mode: AppThemeMode) = repository.updateThemeMode(mode)
    suspend fun updateVibrate(enabled: Boolean) = repository.updateVibrate(enabled)
    suspend fun updateBeep(enabled: Boolean) = repository.updateBeep(enabled)
    suspend fun updateAutoOpen(enabled: Boolean) = repository.updateAutoOpen(enabled)
    suspend fun updateAutoCopy(enabled: Boolean) = repository.updateAutoCopy(enabled)
    suspend fun updateAppLock(enabled: Boolean) = repository.updateAppLock(enabled)
    suspend fun updateBiometric(enabled: Boolean) = repository.updateBiometric(enabled)
    suspend fun updateLockTimeout(seconds: Long) = repository.updateLockTimeout(seconds)
    suspend fun updatePin(pin: String?) = repository.updatePinCode(pin)
    suspend fun completeOnboarding() = repository.setOnboardingCompleted(true)
    suspend fun updateDynamicColor(enabled: Boolean) = repository.updateDynamicColor(enabled)
    suspend fun updateAutoScan(enabled: Boolean) = repository.updateAutoScan(enabled)
    suspend fun updateLanguage(lang: String) = repository.updateLanguage(lang)
    suspend fun updateSaveScanHistory(enabled: Boolean) = repository.updateSaveScanHistory(enabled)
    suspend fun updateSendAnonymousAnalytics(enabled: Boolean) = repository.updateSendAnonymousAnalytics(enabled)
    suspend fun updateSafeUrlDetection(enabled: Boolean) = repository.updateSafeUrlDetection(enabled)
    suspend fun updateSuspiciousQrWarning(enabled: Boolean) = repository.updateSuspiciousQrWarning(enabled)
    suspend fun updateClipboardProtection(enabled: Boolean) = repository.updateClipboardProtection(enabled)
}

class GetAnalyticsSummaryUseCase(
    private val historyRepository: HistoryRepository,
    private val qrRepository: QrGeneratorRepository
) {
    operator fun invoke(
        timeRange: String = "Semua Waktu",
        interval: String = "Harian"
    ): Flow<AnalyticsSummary> {
        return combine(
            historyRepository.getAllHistory(),
            historyRepository.getScansTodayCount(),
            qrRepository.getAllUserQrs()
        ) { historyList, scansToday, userQrs ->
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()

            val cutoffTime = when (timeRange) {
                "7 Hari Terakhir" -> now - (7L * 24 * 60 * 60 * 1000)
                "30 Hari Terakhir" -> now - (30L * 24 * 60 * 60 * 1000)
                "Bulan Ini" -> {
                    calendar.apply {
                        set(java.util.Calendar.DAY_OF_MONTH, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                "Semua Waktu" -> 0L
                else -> now - (7L * 24 * 60 * 60 * 1000)
            }

            // Filter riwayat scan berdasarkan rentang waktu yang dipilih
            val filteredHistory = if (cutoffTime > 0L) {
                historyList.filter { it.createdAt >= cutoffTime }
            } else {
                historyList
            }

            // Filter QR Code yang dibuat/diupdate dalam rentang waktu yang dipilih
            val filteredUserQrs = if (cutoffTime > 0L) {
                userQrs.filter { it.createdAt >= cutoffTime || it.updatedAt >= cutoffTime }
            } else {
                userQrs
            }

            val scansByType = filteredHistory.groupBy { it.type }.mapValues { it.value.size }
            val topType = scansByType.maxByOrNull { it.value }?.key ?: (userQrs.firstOrNull()?.type ?: QrType.WEBSITE)
            val totalFavorites = filteredHistory.count { it.isFavorite } + userQrs.count { it.isFavorite }

            val totalScans = filteredHistory.size + filteredUserQrs.sumOf { it.scanCount }
            val uniqueVisitors = if (filteredHistory.isNotEmpty() || filteredUserQrs.isNotEmpty()) {
                val distinctScanContents = filteredHistory.map { it.content }.toSet()
                val distinctQrContents = filteredUserQrs.map { it.content }.toSet()
                (distinctScanContents + distinctQrContents).size.coerceAtLeast(1)
            } else {
                0
            }

            val activeCount = userQrs.size
            val daysInPeriod = when (timeRange) {
                "7 Hari Terakhir" -> 7
                "30 Hari Terakhir" -> 30
                "Bulan Ini" -> maxOf(1, java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH))
                "Semua Waktu" -> {
                    val oldestScan = historyList.minOfOrNull { it.createdAt } ?: now
                    val diffDays = ((now - oldestScan) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                    diffDays
                }
                else -> 7
            }
            val dailyAverage = if (totalScans > 0) maxOf(1, totalScans / daysInPeriod) else 0

            // 1. Aktivitas Harian (Sen s/d Min) atau Mingguan (M1 s/d M4)
            val daysOrder = listOf(
                java.util.Calendar.MONDAY to "Sen",
                java.util.Calendar.TUESDAY to "Sel",
                java.util.Calendar.WEDNESDAY to "Rab",
                java.util.Calendar.THURSDAY to "Kam",
                java.util.Calendar.FRIDAY to "Jum",
                java.util.Calendar.SATURDAY to "Sab",
                java.util.Calendar.SUNDAY to "Min"
            )

            val dayCounts = mutableMapOf<String, Int>()
            daysOrder.forEach { (_, name) -> dayCounts[name] = 0 }

            val scanCal = java.util.Calendar.getInstance()
            filteredHistory.forEach { item ->
                scanCal.timeInMillis = item.createdAt
                val dow = scanCal.get(java.util.Calendar.DAY_OF_WEEK)
                val dayName = when (dow) {
                    java.util.Calendar.MONDAY -> "Sen"
                    java.util.Calendar.TUESDAY -> "Sel"
                    java.util.Calendar.WEDNESDAY -> "Rab"
                    java.util.Calendar.THURSDAY -> "Kam"
                    java.util.Calendar.FRIDAY -> "Jum"
                    java.util.Calendar.SATURDAY -> "Sab"
                    java.util.Calendar.SUNDAY -> "Min"
                    else -> "Sen"
                }
                dayCounts[dayName] = (dayCounts[dayName] ?: 0) + 1
            }

            val maxDayCount = dayCounts.values.maxOrNull() ?: 0

            val dailyActivities = if (interval == "Mingguan") {
                val weekBuckets = IntArray(4) { 0 }
                val weekMillis = 7L * 24 * 60 * 60 * 1000
                filteredHistory.forEach { item ->
                    val diff = (now - item.createdAt).coerceAtLeast(0)
                    val weekIndex = (diff / weekMillis).toInt()
                    if (weekIndex in 0..3) {
                        weekBuckets[3 - weekIndex]++
                    }
                }
                val maxWeek = weekBuckets.maxOrNull() ?: 0
                listOf("M1", "M2", "M3", "M4").mapIndexed { idx, label ->
                    val count = weekBuckets[idx]
                    val ratio = if (maxWeek > 0) (count.toFloat() / maxWeek).coerceIn(0.08f, 1f) else 0.05f
                    com.scanflow.qr.domain.model.DayActivity(
                        dayName = label,
                        scanCount = count,
                        heightRatio = ratio,
                        isPeak = count == maxWeek && count > 0
                    )
                }
            } else {
                daysOrder.map { (_, name) ->
                    val count = dayCounts[name] ?: 0
                    val ratio = if (maxDayCount > 0) (count.toFloat() / maxDayCount).coerceIn(0.08f, 1f) else 0.05f
                    com.scanflow.qr.domain.model.DayActivity(
                        dayName = name,
                        scanCount = count,
                        heightRatio = ratio,
                        isPeak = count == maxDayCount && count > 0
                    )
                }
            }

            val busiestDayText = if (interval == "Mingguan") {
                val peakWeek = dailyActivities.maxByOrNull { it.scanCount }
                if (peakWeek != null && peakWeek.scanCount > 0) {
                    "Minggu ${peakWeek.dayName} (${peakWeek.scanCount} scan)"
                } else {
                    "Belum ada data scan"
                }
            } else {
                val peakDay = dailyActivities.maxByOrNull { it.scanCount }
                val fullDayName = when (peakDay?.dayName) {
                    "Sen" -> "Senin"
                    "Sel" -> "Selasa"
                    "Rab" -> "Rabu"
                    "Kam" -> "Kamis"
                    "Jum" -> "Jumat"
                    "Sab" -> "Sabtu"
                    "Min" -> "Minggu"
                    else -> ""
                }
                if (peakDay != null && peakDay.scanCount > 0) {
                    "$fullDayName (${peakDay.scanCount} scan)"
                } else {
                    "Belum ada data scan"
                }
            }

            // 2. Top QRs (dinamis dari User QRs dan riwayat scan pengguna)
            val computedTopQrs = if (userQrs.isNotEmpty() && userQrs.any { it.scanCount > 0 }) {
                val sorted = userQrs.sortedByDescending { it.scanCount }.take(3)
                val totalTopScans = sorted.sumOf { it.scanCount }.coerceAtLeast(1)
                sorted.map { qr ->
                    val share = (qr.scanCount.toFloat() / totalTopScans * 100).toInt()
                    val bar = (qr.scanCount.toFloat() / totalTopScans * 80).toInt().coerceIn(10, 95)
                    com.scanflow.qr.domain.model.TopQrAnalyticsItem(
                        title = qr.title.ifBlank { "QR Code #${qr.id}" },
                        type = qr.type,
                        subtitle = qr.content.take(24) + " • " + qr.type.displayName,
                        scanCount = maxOf(qr.scanCount, 1),
                        sharePercent = maxOf(share, 1),
                        barPercent = maxOf(bar, 10)
                    )
                }
            } else if (filteredHistory.isNotEmpty()) {
                val groupedByContent = filteredHistory.groupBy { it.content }
                val topItems = groupedByContent.entries
                    .sortedByDescending { it.value.size }
                    .take(3)
                val totalHistoryScans = filteredHistory.size.coerceAtLeast(1)
                topItems.map { entry ->
                    val firstItem = entry.value.first()
                    val count = entry.value.size
                    val share = (count.toFloat() / totalHistoryScans * 100).toInt()
                    val bar = (count.toFloat() / totalHistoryScans * 80).toInt().coerceIn(10, 95)
                    com.scanflow.qr.domain.model.TopQrAnalyticsItem(
                        title = firstItem.title.ifBlank { "${firstItem.type.displayName} Scan" },
                        type = firstItem.type,
                        subtitle = firstItem.content.take(24) + " • " + firstItem.type.displayName,
                        scanCount = count,
                        sharePercent = maxOf(share, 1),
                        barPercent = maxOf(bar, 10)
                    )
                }
            } else {
                emptyList()
            }

            // 3. Top Locations
            val locationCounts = mutableMapOf<String, Int>()
            filteredHistory.forEach { item ->
                val text = "${item.title} ${item.content}".lowercase()
                val detectedLocation = when {
                    item.type == QrType.LOCATION -> {
                        if (item.title.isNotBlank() && item.title != "Lokasi") item.title
                        else "Lokasi Tersemat (GPS)"
                    }
                    text.contains("jakarta") -> "DKI Jakarta"
                    text.contains("surabaya") -> "Surabaya"
                    text.contains("bandung") -> "Bandung"
                    text.contains("medan") -> "Medan"
                    text.contains("semarang") -> "Semarang"
                    text.contains("jogja") || text.contains("yogyakarta") -> "D.I. Yogyakarta"
                    text.contains("bali") || text.contains("denpasar") -> "Bali"
                    text.contains("tangerang") -> "Tangerang"
                    text.contains("bekasi") -> "Bekasi"
                    text.contains("bogor") -> "Bogor"
                    item.type == QrType.WIFI -> "Jaringan Wi-Fi Lokal"
                    item.type == QrType.WEBSITE -> "Domain Web Publik"
                    else -> "Pindai Langsung (Lokal)"
                }
                locationCounts[detectedLocation] = (locationCounts[detectedLocation] ?: 0) + 1
            }

            val totalLocationScans = locationCounts.values.sum()
            val topLocations = if (totalLocationScans > 0) {
                locationCounts.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .mapIndexed { index, entry ->
                        val pct = ((entry.value.toFloat() / totalLocationScans) * 100).toInt().coerceAtLeast(1)
                        com.scanflow.qr.domain.model.LocationStats(
                            rank = index + 1,
                            cityName = entry.key,
                            percent = pct
                        )
                    }
            } else {
                emptyList()
            }

            // 4. Hourly Breakdown
            val hourlyBuckets = IntArray(6) { 0 }
            val hourCal = java.util.Calendar.getInstance()
            filteredHistory.forEach { item ->
                hourCal.timeInMillis = item.createdAt
                val hour = hourCal.get(java.util.Calendar.HOUR_OF_DAY)
                when (hour) {
                    in 0..3 -> hourlyBuckets[0]++
                    in 4..7 -> hourlyBuckets[1]++
                    in 8..11 -> hourlyBuckets[2]++
                    in 12..15 -> hourlyBuckets[3]++
                    in 16..19 -> hourlyBuckets[4]++
                    else -> hourlyBuckets[5]++
                }
            }

            val timeWindows = listOf(
                "00:00 - 04:00",
                "04:00 - 08:00",
                "08:00 - 12:00",
                "12:00 - 16:00",
                "16:00 - 20:00",
                "20:00 - 24:00"
            )
            val totalHourlyScans = hourlyBuckets.sum()
            val hourlyBreakdown = timeWindows.mapIndexed { index, window ->
                val count = hourlyBuckets[index]
                val percent = if (totalHourlyScans > 0) {
                    (count * 100) / totalHourlyScans
                } else {
                    0
                }
                com.scanflow.qr.domain.model.HourlyStats(
                    timeWindow = window,
                    scanCount = count,
                    percent = percent
                )
            }

            // 5. Device Stats
            val iosCount = filteredHistory.count {
                it.content.contains("apps.apple.com", ignoreCase = true) ||
                it.content.contains("itunes.apple.com", ignoreCase = true) ||
                it.content.startsWith("itms-apps://", ignoreCase = true)
            }
            val androidCount = filteredHistory.size - iosCount
            val totalDeviceScans = filteredHistory.size
            val (iosPercent, androidPercent) = if (totalDeviceScans > 0) {
                val androidPct = ((androidCount.toFloat() / totalDeviceScans) * 100).toInt()
                val iosPct = 100 - androidPct
                iosPct to androidPct
            } else {
                0 to 0
            }

            AnalyticsSummary(
                totalScans = totalScans,
                totalCreated = userQrs.size,
                totalFavorites = totalFavorites,
                scansToday = scansToday,
                topScanType = topType,
                scansByType = scansByType,
                uniqueVisitors = uniqueVisitors,
                activeQrCount = activeCount,
                pausedQrCount = 0,
                dailyAverage = dailyAverage,
                dailyActivities = dailyActivities,
                busiestDayText = busiestDayText,
                topQrs = computedTopQrs,
                deviceStats = com.scanflow.qr.domain.model.DeviceStats(
                    iosPercent = iosPercent,
                    iosCount = iosCount,
                    androidPercent = androidPercent,
                    androidCount = androidCount
                ),
                topLocations = topLocations,
                hourlyBreakdown = hourlyBreakdown
            )
        }
    }
}
