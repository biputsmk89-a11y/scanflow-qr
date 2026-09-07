package com.scanflow.qr.result

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.di.FakeHistoryRepository
import com.scanflow.qr.di.FakeQrGeneratorRepository
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import com.scanflow.qr.feature.result.ScanResultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var qrRepository: FakeQrGeneratorRepository
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var viewModel: ScanResultViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = FakeHistoryRepository()
        qrRepository = FakeQrGeneratorRepository()
        toggleFavoriteUseCase = ToggleFavoriteUseCase(historyRepository, qrRepository)
        viewModel = ScanResultViewModel(historyRepository, toggleFavoriteUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadScanResult with valid URL populates parsed data and security assessment`() = runTest(testDispatcher) {
        val scan = ScanHistoryItem(
            id = 42L,
            content = "https://scanflow.app",
            format = "QR_CODE",
            type = QrType.WEBSITE,
            title = "ScanFlow",
            createdAt = 1000L,
            isFavorite = false
        )
        historyRepository.emitItems(listOf(scan))

        viewModel.loadScanResult(42L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.scanItem).isEqualTo(scan)
        assertThat(state.parsedData).isNotNull()
        assertThat(state.parsedData?.type).isEqualTo(QrType.WEBSITE)
        assertThat(state.securityAssessment).isNotNull()
        assertThat(state.securityAssessment?.isHttps).isTrue()
    }

    @Test
    fun `loadScanResult with nonexistent ID sets state to not loading and item null`() = runTest(testDispatcher) {
        viewModel.loadScanResult(999L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.scanItem).isNull()
        assertThat(state.parsedData).isNull()
    }

    @Test
    fun `toggleFavorite toggles favorite flag for current scan`() = runTest(testDispatcher) {
        val scan = ScanHistoryItem(
            id = 42L,
            content = "Hello Text",
            format = "QR_CODE",
            type = QrType.TEXT,
            title = "Hello",
            createdAt = 1000L,
            isFavorite = false
        )
        historyRepository.emitItems(listOf(scan))

        viewModel.loadScanResult(42L)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.scanItem?.isFavorite).isFalse()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.scanItem?.isFavorite).isTrue()
        val repoItem = historyRepository.getHistoryItemById(42L)
        assertThat(repoItem?.isFavorite).isTrue()
    }

    @Test
    fun `deleteScan deletes item from repository and invokes callback`() = runTest(testDispatcher) {
        val scan = ScanHistoryItem(
            id = 42L,
            content = "Delete me",
            format = "QR_CODE",
            type = QrType.TEXT,
            title = "Delete me",
            createdAt = 1000L,
            isFavorite = false
        )
        historyRepository.emitItems(listOf(scan))

        viewModel.loadScanResult(42L)
        advanceUntilIdle()

        var onDeletedCalled = false
        viewModel.deleteScan {
            onDeletedCalled = true
        }
        advanceUntilIdle()

        assertThat(onDeletedCalled).isTrue()
        assertThat(viewModel.uiState.value.isDeleted).isTrue()
        val repoItem = historyRepository.getHistoryItemById(42L)
        assertThat(repoItem).isNull()
    }

    @Test
    fun `loadScanResult with WiFi QR populates SSID and password correctly`() = runTest(testDispatcher) {
        val scan = ScanHistoryItem(
            id = 55L,
            content = "WIFI:T:WPA;S:MyHomeWiFi;P:SuperSecretPass;H:false;;",
            format = "QR_CODE",
            type = QrType.WIFI,
            title = "MyHomeWiFi",
            createdAt = 2000L,
            isFavorite = false
        )
        historyRepository.emitItems(listOf(scan))

        viewModel.loadScanResult(55L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.parsedData).isNotNull()
        assertThat(state.parsedData?.type).isEqualTo(QrType.WIFI)
        assertThat(state.parsedData?.displayDetails?.get("Network (SSID)")).isEqualTo("MyHomeWiFi")
        assertThat(state.parsedData?.displayDetails?.get("Password")).isEqualTo("SuperSecretPass")
        assertThat(state.wifiConnectionStatus).isEqualTo(com.scanflow.qr.core.utils.WifiConnectionStatus.IDLE)
    }

    @Test
    fun `loadScanResult with 1D Barcode parses type as BARCODE and displayDetails`() = runTest(testDispatcher) {
        val scan = ScanHistoryItem(
            id = 77L,
            content = "8992761136015",
            format = "EAN_13",
            type = QrType.BARCODE,
            title = "Barcode (EAN_13)",
            createdAt = 3000L,
            isFavorite = false
        )
        historyRepository.emitItems(listOf(scan))

        viewModel.loadScanResult(77L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.parsedData).isNotNull()
        assertThat(state.parsedData?.type).isEqualTo(QrType.BARCODE)
        assertThat(state.parsedData?.displayDetails?.get("Code")).isEqualTo("8992761136015")
        assertThat(state.parsedData?.displayDetails?.get("Symbology")).isEqualTo("EAN_13")
    }

    @Test
    fun `loadScanResult with vCard parses structured contact fields`() = runTest(testDispatcher) {
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nN:Santoso;Budi;;;\nFN:Budi Santoso\nTEL:+628123456789\nEMAIL:budi@scanflow.app\nORG:PT ScanFlow Indonesia\nEND:VCARD"
        val scan = ScanHistoryItem(
            id = 88L,
            content = vcard,
            format = "QR_CODE",
            type = QrType.CONTACT,
            title = "Budi Santoso",
            createdAt = 4000L,
            isFavorite = false
        )
        historyRepository.emitItems(listOf(scan))

        viewModel.loadScanResult(88L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.parsedData).isNotNull()
        assertThat(state.parsedData?.type).isEqualTo(QrType.CONTACT)
        assertThat(state.parsedData?.displayDetails?.get("Name")).contains("Budi Santoso")
        assertThat(state.parsedData?.displayDetails?.get("Phone")).isEqualTo("+628123456789")
        assertThat(state.parsedData?.displayDetails?.get("Email")).isEqualTo("budi@scanflow.app")
        assertThat(state.parsedData?.displayDetails?.get("Organization")).isEqualTo("PT ScanFlow Indonesia")
    }
}
