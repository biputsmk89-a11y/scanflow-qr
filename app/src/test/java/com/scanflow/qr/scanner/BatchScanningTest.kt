package com.scanflow.qr.scanner

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.ScanRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.ParseQrCodeUseCase
import com.scanflow.qr.domain.usecase.SaveScanResultUseCase
import com.scanflow.qr.feature.scanner.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class FakeScanRepository : ScanRepository {
    private var idCounter = 1L
    val savedItems = mutableListOf<QrCodeData>()

    override fun parseScannedContent(rawContent: String, format: String): QrCodeData {
        return QrCodeData(
            rawContent = rawContent,
            format = format,
            type = if (rawContent.startsWith("http")) QrType.WEBSITE else QrType.TEXT,
            title = "Item: $rawContent",
            displayDetails = mapOf("Content" to rawContent)
        )
    }

    override suspend fun saveScanResult(qrCodeData: QrCodeData): Long {
        savedItems.add(qrCodeData)
        return idCounter++
    }
}

class FakeSettingsRepository : SettingsRepository {
    private val _settings = MutableStateFlow(
        AppSettings(
            themeMode = AppThemeMode.SYSTEM,
            vibrateOnScan = false,
            beepOnScan = false,
            autoOpenUrl = false,
            autoCopyToClipboard = false
        )
    )
    override val settingsFlow: Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun updateThemeMode(themeMode: AppThemeMode) {}
    override suspend fun updateVibrate(enabled: Boolean) {}
    override suspend fun updateBeep(enabled: Boolean) {}
    override suspend fun updateAutoOpen(enabled: Boolean) {}
    override suspend fun updateAutoCopy(enabled: Boolean) {}
    override suspend fun updateAppLock(enabled: Boolean) {}
    override suspend fun updateBiometric(enabled: Boolean) {}
    override suspend fun updatePinCode(pin: String?) {}
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
}

class FakeHistoryRepository : HistoryRepository {
    val deletedIds = mutableListOf<Long>()

    override fun getAllHistory(): Flow<List<ScanHistoryItem>> = flowOf(emptyList())
    override fun getFavoriteHistory(): Flow<List<ScanHistoryItem>> = flowOf(emptyList())
    override fun searchHistory(query: String): Flow<List<ScanHistoryItem>> = flowOf(emptyList())
    override fun getHistoryByType(type: QrType): Flow<List<ScanHistoryItem>> = flowOf(emptyList())
    override fun getRecentHistory(limit: Int): Flow<List<ScanHistoryItem>> = flowOf(emptyList())
    override fun getTotalScanCount(): Flow<Int> = flowOf(0)
    override fun getScansTodayCount(): Flow<Int> = flowOf(0)
    override suspend fun getHistoryItemById(id: Long): ScanHistoryItem? = null
    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {}
    override suspend fun deleteHistoryItem(id: Long) {
        deletedIds.add(id)
    }
    override suspend fun deleteHistoryItems(ids: List<Long>) {
        deletedIds.addAll(ids)
    }
    override suspend fun clearAllHistory() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class BatchScanningTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var scanRepository: FakeScanRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        scanRepository = FakeScanRepository()
        settingsRepository = FakeSettingsRepository()
        historyRepository = FakeHistoryRepository()

        viewModel = ScannerViewModel(
            parseQrCodeUseCase = ParseQrCodeUseCase(scanRepository),
            saveScanResultUseCase = SaveScanResultUseCase(scanRepository),
            getSettingsUseCase = GetSettingsUseCase(settingsRepository),
            deleteHistoryUseCase = DeleteHistoryUseCase(historyRepository)
        ).apply {
            scanDebounceMs = 0L
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has batch mode disabled and empty batch`() {
        val state = viewModel.uiState.value
        assertThat(state.isBatchMode).isFalse()
        assertThat(state.batchCount).isEqualTo(0)
        assertThat(state.batchItems).isEmpty()
        assertThat(state.allowDuplicates).isFalse()
    }

    @Test
    fun `toggleBatchMode switches batch mode correctly`() {
        viewModel.toggleBatchMode()
        assertThat(viewModel.uiState.value.isBatchMode).isTrue()

        viewModel.toggleBatchMode()
        assertThat(viewModel.uiState.value.isBatchMode).isFalse()
    }

    @Test
    fun `toggleAllowDuplicates toggles allowDuplicates flag`() {
        assertThat(viewModel.uiState.value.allowDuplicates).isFalse()

        viewModel.toggleAllowDuplicates()
        assertThat(viewModel.uiState.value.allowDuplicates).isTrue()

        viewModel.toggleAllowDuplicates()
        assertThat(viewModel.uiState.value.allowDuplicates).isFalse()
    }

    @Test
    fun `continuous batch scanning accumulates multiple barcodes without stopping`() = runTest {
        viewModel.toggleBatchMode()

        // Scan 3 distinct barcodes
        viewModel.onBarcodeDetected(rawValue = "SISWA-SMK-001", format = "QR_CODE")
        advanceUntilIdle()

        viewModel.onBarcodeDetected(rawValue = "SISWA-SMK-002", format = "QR_CODE")
        advanceUntilIdle()

        viewModel.onBarcodeDetected(rawValue = "SISWA-SMK-003", format = "QR_CODE")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.batchCount).isEqualTo(3)
        assertThat(state.batchItems).hasSize(3)
        // Camera stays active in batch mode!
        assertThat(state.isScanningActive).isTrue()
        assertThat(state.batchItems.map { it.data.rawContent }).containsExactly(
            "SISWA-SMK-003", "SISWA-SMK-002", "SISWA-SMK-001"
        ).inOrder()
    }

    @Test
    fun `duplicate rejection ignores duplicate code when allowDuplicates is false`() = runTest {
        viewModel.toggleBatchMode()
        // allowDuplicates is false by default

        viewModel.onBarcodeDetected(rawValue = "TIKET-VIP-999", format = "QR_CODE")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.batchCount).isEqualTo(1)

        // Attempt scanning duplicate code
        viewModel.onBarcodeDetected(rawValue = "TIKET-VIP-999", format = "QR_CODE")
        testScheduler.runCurrent()

        // Batch count should still be 1!
        val state = viewModel.uiState.value
        assertThat(state.batchCount).isEqualTo(1)
        assertThat(state.isDuplicateWarning).isTrue()
        assertThat(state.scanBannerMessage).contains("Duplikat diabaikan")
    }

    @Test
    fun `duplicate rejection allows duplicate code when allowDuplicates is true`() = runTest {
        viewModel.toggleBatchMode()
        viewModel.toggleAllowDuplicates() // allowDuplicates = true (Inventory/Warehouse mode)

        viewModel.onBarcodeDetected(rawValue = "BARANG-GUDANG-A1", format = "CODE_128")
        advanceUntilIdle()

        viewModel.onBarcodeDetected(rawValue = "BARANG-GUDANG-A1", format = "CODE_128")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.batchCount).isEqualTo(2)
        assertThat(state.batchItems).hasSize(2)
    }

    @Test
    fun `removeItemFromBatch removes item and deletes it from history`() = runTest {
        viewModel.toggleBatchMode()

        viewModel.onBarcodeDetected(rawValue = "ITEM-1", format = "QR_CODE")
        advanceUntilIdle()
        viewModel.onBarcodeDetected(rawValue = "ITEM-2", format = "QR_CODE")
        advanceUntilIdle()

        val item1ScanId = viewModel.uiState.value.batchItems.first { it.data.rawContent == "ITEM-1" }.scanId

        viewModel.removeItemFromBatch(item1ScanId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.batchCount).isEqualTo(1)
        assertThat(state.batchItems.none { it.scanId == item1ScanId }).isTrue()
        assertThat(historyRepository.deletedIds).contains(item1ScanId)
    }

    @Test
    fun `clearBatch resets batch items and count`() = runTest {
        viewModel.toggleBatchMode()
        viewModel.onBarcodeDetected(rawValue = "ITEM-A", format = "QR_CODE")
        advanceUntilIdle()
        viewModel.onBarcodeDetected(rawValue = "ITEM-B", format = "QR_CODE")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.batchCount).isEqualTo(2)

        viewModel.clearBatch()
        val state = viewModel.uiState.value
        assertThat(state.batchCount).isEqualTo(0)
        assertThat(state.batchItems).isEmpty()
        assertThat(state.lastBatchItem).isNull()
    }

    @Test
    fun `getBatchCsvExportText generates valid CSV formatted string`() = runTest {
        viewModel.toggleBatchMode()
        viewModel.onBarcodeDetected(rawValue = "2401001", format = "QR_CODE")
        advanceUntilIdle()
        viewModel.onBarcodeDetected(rawValue = "2401002", format = "QR_CODE")
        advanceUntilIdle()

        val csv = viewModel.getBatchCsvExportText()
        val lines = csv.trim().split("\n")

        assertThat(lines).hasSize(3) // 1 header + 2 items
        assertThat(lines[0]).isEqualTo("No,Waktu,Format,Tipe,Judul,Konten")
        assertThat(lines[1]).contains("2401001")
        assertThat(lines[2]).contains("2401002")
    }

    @Test
    fun `getBatchPlainTextSummary generates clean readable report`() = runTest {
        viewModel.toggleBatchMode()
        viewModel.onBarcodeDetected(rawValue = "ABSEN-BUDI", format = "QR_CODE")
        advanceUntilIdle()

        val summary = viewModel.getBatchPlainTextSummary()
        assertThat(summary).contains("Laporan Sesi Pindai Batch")
        assertThat(summary).contains("Total Item: 1")
        assertThat(summary).contains("ABSEN-BUDI")
    }
}
