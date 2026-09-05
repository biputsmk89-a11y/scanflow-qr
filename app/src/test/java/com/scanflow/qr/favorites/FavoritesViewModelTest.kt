package com.scanflow.qr.favorites

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.di.FakeFavoriteRepository
import com.scanflow.qr.di.FakeHistoryRepository
import com.scanflow.qr.di.FakeQrGeneratorRepository
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import com.scanflow.qr.feature.favorites.FavoritesViewModel
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
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var qrRepository: FakeQrGeneratorRepository
    private lateinit var favoriteRepository: FakeFavoriteRepository
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var viewModel: FavoritesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = FakeHistoryRepository()
        qrRepository = FakeQrGeneratorRepository()
        favoriteRepository = FakeFavoriteRepository(historyRepository, qrRepository)
        toggleFavoriteUseCase = ToggleFavoriteUseCase(historyRepository, qrRepository)

        viewModel = FavoritesViewModel(
            favoriteRepository = favoriteRepository,
            toggleFavoriteUseCase = toggleFavoriteUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits only favorite scans and favorite created QRs`() = runTest(testDispatcher) {
        historyRepository.emitItems(
            listOf(
                ScanHistoryItem(id = 1L, content = "http://a.com", format = "QR", type = QrType.WEBSITE, title = "A", isFavorite = true),
                ScanHistoryItem(id = 2L, content = "http://b.com", format = "QR", type = QrType.WEBSITE, title = "B", isFavorite = false)
            )
        )
        qrRepository.saveUserQr(
            UserQrCode(id = 10L, type = QrType.TEXT, title = "My Note", content = "Secret", isFavorite = true)
        )
        qrRepository.saveUserQr(
            UserQrCode(id = 20L, type = QrType.WIFI, title = "Office Wifi", content = "WIFI:...", isFavorite = false)
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.favoriteScans).hasSize(1)
        assertThat(state.favoriteScans.first().id).isEqualTo(1L)
        assertThat(state.favoriteUserQrs).hasSize(1)
        assertThat(state.favoriteUserQrs.first().id).isEqualTo(10L)
    }

    @Test
    fun `toggleScanFavorite unfavorites item and removes it from favorite scans flow`() = runTest(testDispatcher) {
        historyRepository.emitItems(
            listOf(
                ScanHistoryItem(id = 1L, content = "http://a.com", format = "QR", type = QrType.WEBSITE, title = "A", isFavorite = true)
            )
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.favoriteScans).hasSize(1)

        // Toggle favorite off (current is true)
        viewModel.toggleScanFavorite(1L, isFavorite = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.favoriteScans).isEmpty()
    }

    @Test
    fun `toggleUserQrFavorite unfavorites user QR and updates favorite user QRs flow`() = runTest(testDispatcher) {
        qrRepository.saveUserQr(
            UserQrCode(id = 10L, type = QrType.TEXT, title = "My Note", content = "Secret", isFavorite = true)
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.favoriteUserQrs).hasSize(1)

        // Toggle favorite off (current is true)
        viewModel.toggleUserQrFavorite(10L, isFavorite = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.favoriteUserQrs).isEmpty()
    }
}
