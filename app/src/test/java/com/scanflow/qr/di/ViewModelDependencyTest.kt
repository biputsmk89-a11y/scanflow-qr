package com.scanflow.qr.di

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import com.scanflow.qr.feature.history.HistoryViewModel
import com.scanflow.qr.feature.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelDependencyTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeHistoryRepository: FakeHistoryRepository
    private lateinit var fakeQrGeneratorRepository: FakeQrGeneratorRepository

    private lateinit var getHistoryUseCase: GetHistoryUseCase
    private lateinit var getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var deleteHistoryUseCase: DeleteHistoryUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeHistoryRepository = FakeHistoryRepository()
        fakeQrGeneratorRepository = FakeQrGeneratorRepository()

        getHistoryUseCase = GetHistoryUseCase(fakeHistoryRepository)
        getAnalyticsSummaryUseCase = GetAnalyticsSummaryUseCase(fakeHistoryRepository, fakeQrGeneratorRepository)
        toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeHistoryRepository, fakeQrGeneratorRepository)
        deleteHistoryUseCase = DeleteHistoryUseCase(fakeHistoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun homeViewModel_receivesInjectedDependencies_andEmitsInitialState() = runTest {
        val sampleItem = ScanHistoryItem(
            id = 1L,
            title = "WiFi SMK",
            content = "WIFI:S:SMK_Network;;",
            type = QrType.WIFI,
            format = "QR_CODE",
            createdAt = System.currentTimeMillis(),
            isFavorite = false,
            isSecure = true
        )
        fakeHistoryRepository.emitItems(listOf(sampleItem))

        val homeViewModel = HomeViewModel(
            getHistoryUseCase = getHistoryUseCase,
            getAnalyticsSummaryUseCase = getAnalyticsSummaryUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase
        )

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            homeViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val state = homeViewModel.uiState.value
        assertThat(state.recentScans).hasSize(1)
        assertThat(state.recentScans.first().title).isEqualTo("WiFi SMK")
    }

    @Test
    fun historyViewModel_searchFiltering_worksThroughInjectedRepository() = runTest {
        val items = listOf(
            ScanHistoryItem(
                id = 1L,
                title = "Google Web",
                content = "https://google.com",
                type = QrType.WEBSITE,
                format = "QR_CODE",
                createdAt = System.currentTimeMillis(),
                isFavorite = false,
                isSecure = true
            ),
            ScanHistoryItem(
                id = 2L,
                title = "Lab SMK",
                content = "GEO:12.34,56.78",
                type = QrType.LOCATION,
                format = "QR_CODE",
                createdAt = System.currentTimeMillis(),
                isFavorite = true,
                isSecure = true
            )
        )
        fakeHistoryRepository.emitItems(items)

        val historyViewModel = HistoryViewModel(
            historyRepository = fakeHistoryRepository,
            getHistoryUseCase = getHistoryUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            deleteHistoryUseCase = deleteHistoryUseCase
        )

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            historyViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertThat(historyViewModel.uiState.value.items).hasSize(2)

        historyViewModel.onSearchQueryChanged("Lab")
        advanceUntilIdle()

        assertThat(historyViewModel.uiState.value.searchQuery).isEqualTo("Lab")
    }
}
