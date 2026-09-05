package com.scanflow.qr.history

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.di.FakeHistoryRepository
import com.scanflow.qr.di.FakeQrGeneratorRepository
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import com.scanflow.qr.feature.history.HistoryViewModel
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
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var qrRepository: FakeQrGeneratorRepository
    private lateinit var getHistoryUseCase: GetHistoryUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var deleteHistoryUseCase: DeleteHistoryUseCase
    private lateinit var viewModel: HistoryViewModel

    private val sampleItems = listOf(
        ScanHistoryItem(
            id = 1L,
            content = "https://example.com",
            format = "QR_CODE",
            type = QrType.WEBSITE,
            title = "Example Domain",
            createdAt = 1000L,
            isFavorite = false
        ),
        ScanHistoryItem(
            id = 2L,
            content = "WIFI:T:WPA;S:HomeNetwork;P:pass123;;",
            format = "QR_CODE",
            type = QrType.WIFI,
            title = "Wi-Fi: HomeNetwork",
            createdAt = 2000L,
            isFavorite = true
        ),
        ScanHistoryItem(
            id = 3L,
            content = "tel:+62812345678",
            format = "QR_CODE",
            type = QrType.PHONE,
            title = "Phone: +62812345678",
            createdAt = 3000L,
            isFavorite = false
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = FakeHistoryRepository()
        qrRepository = FakeQrGeneratorRepository()
        getHistoryUseCase = GetHistoryUseCase(historyRepository)
        toggleFavoriteUseCase = ToggleFavoriteUseCase(historyRepository, qrRepository)
        deleteHistoryUseCase = DeleteHistoryUseCase(historyRepository)

        historyRepository.emitItems(sampleItems)

        viewModel = HistoryViewModel(
            historyRepository = historyRepository,
            getHistoryUseCase = getHistoryUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            deleteHistoryUseCase = deleteHistoryUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits all history items initially`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.items).hasSize(3)
        assertThat(state.isSelectionMode).isFalse()
        assertThat(state.selectedIds).isEmpty()
    }

    @Test
    fun `search query filters history items reactively`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("HomeNetwork")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.items).hasSize(1)
        assertThat(state.items.first().title).isEqualTo("Wi-Fi: HomeNetwork")
        assertThat(state.searchQuery).isEqualTo("HomeNetwork")
    }

    @Test
    fun `filter type filters items by specific QrType`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectFilterType(QrType.PHONE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.items).hasSize(1)
        assertThat(state.items.first().type).isEqualTo(QrType.PHONE)
        assertThat(state.selectedFilterType).isEqualTo(QrType.PHONE)
    }

    @Test
    fun `toggle selection enables selection mode and tracks selected item IDs`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.toggleSelection(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSelectionMode).isTrue()
        assertThat(viewModel.uiState.value.selectedIds).containsExactly(1L)

        viewModel.toggleSelection(2L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIds).containsExactly(1L, 2L)

        // Toggle 1L off
        viewModel.toggleSelection(1L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIds).containsExactly(2L)
        assertThat(viewModel.uiState.value.isSelectionMode).isTrue()

        // Toggle 2L off -> selection mode turns false
        viewModel.toggleSelection(2L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
        assertThat(viewModel.uiState.value.isSelectionMode).isFalse()
    }

    @Test
    fun `clearSelection resets selection state`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedIds).isNotEmpty()

        viewModel.clearSelection()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
        assertThat(viewModel.uiState.value.isSelectionMode).isFalse()
    }

    @Test
    fun `deleteSelectedItems removes only chosen items and clears selection`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(3L)
        advanceUntilIdle()

        viewModel.deleteSelectedItems()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.items).hasSize(1)
        assertThat(state.items.first().id).isEqualTo(2L)
        assertThat(state.selectedIds).isEmpty()
        assertThat(state.isSelectionMode).isFalse()
    }

    @Test
    fun `clearAllHistory removes all items`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.clearAllHistory()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).isEmpty()
    }

    @Test
    fun `toggleFavorite updates favorite state in repository`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Toggle item 1 from false to true
        viewModel.toggleFavorite(1L, current = false)
        advanceUntilIdle()

        val updatedItem = historyRepository.getHistoryItemById(1L)
        assertThat(updatedItem?.isFavorite).isTrue()
    }
}
