package com.scanflow.qr.home

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.di.FakeHistoryRepository
import com.scanflow.qr.di.FakeQrGeneratorRepository
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import com.scanflow.qr.feature.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var qrRepository: FakeQrGeneratorRepository
    private lateinit var getHistoryUseCase: GetHistoryUseCase
    private lateinit var getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = FakeHistoryRepository()
        qrRepository = FakeQrGeneratorRepository()
        getHistoryUseCase = GetHistoryUseCase(historyRepository)
        getAnalyticsSummaryUseCase = GetAnalyticsSummaryUseCase(historyRepository, qrRepository)
        toggleFavoriteUseCase = ToggleFavoriteUseCase(historyRepository, qrRepository)

        viewModel = HomeViewModel(
            getHistoryUseCase = getHistoryUseCase,
            getAnalyticsSummaryUseCase = getAnalyticsSummaryUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits recent scans limited to 5 and calculates analytics summary`() = runTest(testDispatcher) {
        val scanItems = (1..10).map { i ->
            ScanHistoryItem(
                id = i.toLong(),
                content = "https://scanflow.app/$i",
                format = "QR_CODE",
                type = QrType.WEBSITE,
                title = "Scan $i",
                createdAt = i * 1000L,
                isFavorite = i % 2 == 0
            )
        }
        historyRepository.emitItems(scanItems)

        qrRepository.saveUserQr(
            UserQrCode(id = 101L, type = QrType.TEXT, title = "Custom Note", content = "Test", isFavorite = true)
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.recentScans).hasSize(5)
        assertThat(state.recentScans.first().id).isEqualTo(1L)
        assertThat(state.analytics.totalScans).isEqualTo(10)
        assertThat(state.analytics.totalCreated).isEqualTo(1)
        assertThat(state.analytics.totalFavorites).isEqualTo(5 + 1) // 5 scans + 1 user QR
    }

    @Test
    fun `toggleFavorite toggles favorite flag for a scan item in Home`() = runTest(testDispatcher) {
        val scanItems = listOf(
            ScanHistoryItem(
                id = 1L,
                content = "https://scanflow.app/1",
                format = "QR_CODE",
                type = QrType.WEBSITE,
                title = "Scan 1",
                createdAt = 1000L,
                isFavorite = false
            )
        )
        historyRepository.emitItems(scanItems)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.toggleFavorite(1L, currentFavorite = false)
        advanceUntilIdle()

        val item = historyRepository.getHistoryItemById(1L)
        assertThat(item?.isFavorite).isTrue()
    }
}
