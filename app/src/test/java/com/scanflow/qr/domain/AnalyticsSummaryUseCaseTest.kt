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
}
