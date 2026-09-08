package com.scanflow.qr.domain

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.di.FakeHistoryRepository
import com.scanflow.qr.di.FakeQrGeneratorRepository
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AnalyticsSummaryUseCaseTest {

    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var qrRepository: FakeQrGeneratorRepository
    private lateinit var getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase

    @Before
    fun setUp() {
        historyRepository = FakeHistoryRepository()
        qrRepository = FakeQrGeneratorRepository()
        getAnalyticsSummaryUseCase = GetAnalyticsSummaryUseCase(historyRepository, qrRepository)
    }

    @Test
    fun `invoke with empty repositories returns default empty summary`() = runTest {
        val summary = getAnalyticsSummaryUseCase().first()

        assertThat(summary.totalScans).isEqualTo(0)
        assertThat(summary.totalCreated).isEqualTo(0)
        assertThat(summary.totalFavorites).isEqualTo(0)
        assertThat(summary.topScanType).isEqualTo(QrType.WEBSITE)
        assertThat(summary.scansByType).isEmpty()
        assertThat(summary.topLocations).isEmpty()
    }

    @Test
    fun `invoke aggregates scan counts, top scan type, and combined favorites correctly`() = runTest {
        val historyItems = listOf(
            ScanHistoryItem(id = 1, content = "http://a.com", format = "QR", type = QrType.WEBSITE, title = "A", isFavorite = true),
            ScanHistoryItem(id = 2, content = "WIFI:1", format = "QR", type = QrType.WIFI, title = "W1", isFavorite = false),
            ScanHistoryItem(id = 3, content = "WIFI:2", format = "QR", type = QrType.WIFI, title = "W2", isFavorite = true),
            ScanHistoryItem(id = 4, content = "WIFI:3", format = "QR", type = QrType.WIFI, title = "W3", isFavorite = false)
        )
        historyRepository.emitItems(historyItems)

        qrRepository.saveUserQr(
            UserQrCode(id = 10, type = QrType.TEXT, title = "T1", content = "C1", isFavorite = true)
        )
        qrRepository.saveUserQr(
            UserQrCode(id = 20, type = QrType.EMAIL, title = "E1", content = "C2", isFavorite = false)
        )

        val summary = getAnalyticsSummaryUseCase().first()

        assertThat(summary.totalScans).isEqualTo(4)
        assertThat(summary.totalCreated).isEqualTo(2)
        // 2 favorites from history + 1 favorite from created QRs = 3
        assertThat(summary.totalFavorites).isEqualTo(3)
        // WIFI appears 3 times, WEBSITE appears 1 time -> topScanType is WIFI
        assertThat(summary.topScanType).isEqualTo(QrType.WIFI)
        assertThat(summary.scansByType[QrType.WIFI]).isEqualTo(3)
        assertThat(summary.scansByType[QrType.WEBSITE]).isEqualTo(1)
    }

    @Test
    fun `invoke with time range filters items by timestamp dynamically`() = runTest {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (1L * 24 * 60 * 60 * 1000)
        val tenDaysAgo = now - (10L * 24 * 60 * 60 * 1000)
        val fortyDaysAgo = now - (40L * 24 * 60 * 60 * 1000)

        val historyItems = listOf(
            ScanHistoryItem(id = 1, content = "http://recent.com", format = "QR", type = QrType.WEBSITE, title = "Recent", createdAt = oneDayAgo),
            ScanHistoryItem(id = 2, content = "http://mid.com", format = "QR", type = QrType.WEBSITE, title = "Mid", createdAt = tenDaysAgo),
            ScanHistoryItem(id = 3, content = "http://old.com", format = "QR", type = QrType.WEBSITE, title = "Old", createdAt = fortyDaysAgo)
        )
        historyRepository.emitItems(historyItems)

        // 7 Hari Terakhir: only oneDayAgo item (id = 1)
        val summary7Days = getAnalyticsSummaryUseCase(timeRange = "7 Hari Terakhir").first()
        assertThat(summary7Days.totalScans).isEqualTo(1)

        // 30 Hari Terakhir: oneDayAgo and tenDaysAgo (id = 1, 2)
        val summary30Days = getAnalyticsSummaryUseCase(timeRange = "30 Hari Terakhir").first()
        assertThat(summary30Days.totalScans).isEqualTo(2)

        // Semua Waktu: all 3 items
        val summaryAllTime = getAnalyticsSummaryUseCase(timeRange = "Semua Waktu").first()
        assertThat(summaryAllTime.totalScans).isEqualTo(3)
    }

    @Test
    fun `invoke with weekly interval returns 4 week buckets and dynamic busiest day`() = runTest {
        val now = System.currentTimeMillis()
        val historyItems = listOf(
            ScanHistoryItem(id = 1, content = "http://test.com", format = "QR", type = QrType.WEBSITE, title = "Test", createdAt = now)
        )
        historyRepository.emitItems(historyItems)

        val summaryWeekly = getAnalyticsSummaryUseCase(timeRange = "Semua Waktu", interval = "Mingguan").first()
        assertThat(summaryWeekly.dailyActivities.map { it.dayName }).containsExactly("M1", "M2", "M3", "M4").inOrder()
        assertThat(summaryWeekly.busiestDayText).contains("Minggu M4")
    }

    @Test
    fun `invoke calculates hourly breakdown and top locations from real scans`() = runTest {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
        val nineAmTime = cal.timeInMillis

        val historyItems = listOf(
            ScanHistoryItem(id = 1, content = "http://example.com", format = "QR", type = QrType.WEBSITE, title = "Diskominfo Surabaya", createdAt = nineAmTime),
            ScanHistoryItem(id = 2, content = "https://apps.apple.com/app/test", format = "QR", type = QrType.WEBSITE, title = "Apple App", createdAt = nineAmTime)
        )
        historyRepository.emitItems(historyItems)

        val summary = getAnalyticsSummaryUseCase(timeRange = "Semua Waktu").first()

        // Hourly breakdown at 08:00 - 12:00 should have 2 scans (100%)
        val stats9Am = summary.hourlyBreakdown.find { it.timeWindow == "08:00 - 12:00" }
        assertThat(stats9Am?.scanCount).isEqualTo(2)
        assertThat(stats9Am?.percent).isEqualTo(100)

        // Device stats: 1 iOS link, 1 Android scan
        assertThat(summary.deviceStats.iosCount).isEqualTo(1)
        assertThat(summary.deviceStats.androidCount).isEqualTo(1)
        assertThat(summary.deviceStats.iosPercent).isEqualTo(50)
        assertThat(summary.deviceStats.androidPercent).isEqualTo(50)

        // Top locations: Surabaya detected
        assertThat(summary.topLocations.any { it.cityName == "Surabaya" }).isTrue()
    }
}
