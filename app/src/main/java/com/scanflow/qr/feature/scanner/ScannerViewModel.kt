package com.scanflow.qr.feature.scanner

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.scanflow.qr.core.utils.SoundHelper
import com.scanflow.qr.core.utils.VibratorHelper
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.ParseQrCodeUseCase
import com.scanflow.qr.domain.usecase.SaveScanResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScannedBatchItem(
    val scanId: Long,
    val data: QrCodeData,
    val scannedAt: Long = System.currentTimeMillis()
)

data class ScannerUiState(
    val isTorchEnabled: Boolean = false,
    val cameraLens: Int = CameraSelector.LENS_FACING_BACK,
    val isScanningActive: Boolean = true,
    val isBatchMode: Boolean = false,
    val allowDuplicates: Boolean = false,
    val isBatchSheetVisible: Boolean = false,
    val batchItems: List<ScannedBatchItem> = emptyList(),
    val batchCount: Int = 0,
    val lastBatchItem: QrCodeData? = null,
    val lastScannedId: Long? = null,
    val scanBannerMessage: String? = null,
    val isDuplicateWarning: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val saveScanResultUseCase: SaveScanResultUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val deleteHistoryUseCase: DeleteHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    internal var scanDebounceMs: Long = 800L
    private var lastScannedContent: String? = null
    private var lastScanTimestamp: Long = 0
    private var bannerJob: Job? = null

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchEnabled = !_uiState.value.isTorchEnabled)
    }

    fun toggleBatchMode() {
        val newMode = !_uiState.value.isBatchMode
        _uiState.value = _uiState.value.copy(
            isBatchMode = newMode,
            scanBannerMessage = null
        )
    }

    fun toggleAllowDuplicates() {
        _uiState.value = _uiState.value.copy(
            allowDuplicates = !_uiState.value.allowDuplicates
        )
    }

    fun setBatchSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isBatchSheetVisible = visible)
    }

    fun clearBatch() {
        _uiState.value = _uiState.value.copy(
            batchCount = 0,
            batchItems = emptyList(),
            lastBatchItem = null,
            scanBannerMessage = null
        )
    }

    fun removeItemFromBatch(scanId: Long) {
        val currentList = _uiState.value.batchItems
        val updatedList = currentList.filterNot { it.scanId == scanId }
        val newLastItem = updatedList.firstOrNull()?.data
        _uiState.value = _uiState.value.copy(
            batchItems = updatedList,
            batchCount = updatedList.size,
            lastBatchItem = newLastItem
        )

        // Asynchronously delete from history repository if provider is available
        deleteHistoryUseCase?.let { useCase ->
            viewModelScope.launch {
                try {
                    useCase.deleteItem(scanId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(scanBannerMessage = null)
    }

    fun switchCamera() {
        val newLens = if (_uiState.value.cameraLens == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.value = _uiState.value.copy(cameraLens = newLens)
    }

    fun onBarcodeDetected(context: Context? = null, rawValue: String, format: String = "QR_CODE", onNavigateResult: (Long) -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        if (!_uiState.value.isScanningActive) return

        val isBatch = _uiState.value.isBatchMode
        val allowDuplicates = _uiState.value.allowDuplicates

        // Duplicate rejection logic in batch mode
        if (isBatch && !allowDuplicates) {
            val isDuplicate = _uiState.value.batchItems.any { it.data.rawContent == rawValue }
            if (isDuplicate) {
                if (rawValue != lastScannedContent || (currentTime - lastScanTimestamp) >= scanDebounceMs) {
                    lastScannedContent = rawValue
                    lastScanTimestamp = currentTime
                    context?.let { VibratorHelper.vibrateWarning(it) }
                    showBanner("⚠️ Duplikat diabaikan: ${rawValue.take(24)}...", isWarning = true)
                }
                return
            }
        }

        // Prevent duplicate frame spam within debounce interval for identical barcode
        if (rawValue == lastScannedContent && (currentTime - lastScanTimestamp) < scanDebounceMs) {
            return
        }

        lastScannedContent = rawValue
        lastScanTimestamp = currentTime

        if (!isBatch) {
            _uiState.value = _uiState.value.copy(isScanningActive = false)
        }

        viewModelScope.launch {
            val settings = getSettingsUseCase().first()
            if (settings.vibrateOnScan) context?.let { VibratorHelper.vibrateSuccess(it) }
            if (settings.beepOnScan) SoundHelper.playBeep()

            val parsedData = parseQrCodeUseCase(rawValue, format)
            val savedId = saveScanResultUseCase(parsedData)

            if (isBatch) {
                val newItem = ScannedBatchItem(
                    scanId = savedId,
                    data = parsedData,
                    scannedAt = currentTime
                )
                val updatedList = listOf(newItem) + _uiState.value.batchItems
                _uiState.value = _uiState.value.copy(
                    batchCount = updatedList.size,
                    batchItems = updatedList,
                    lastBatchItem = parsedData,
                    lastScannedId = savedId
                )
                showBanner("✅ #${updatedList.size}: ${parsedData.title}", isWarning = false)
            } else {
                _uiState.value = _uiState.value.copy(lastScannedId = savedId)
                onNavigateResult(savedId)
            }
        }
    }

    private fun showBanner(message: String, isWarning: Boolean) {
        bannerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            scanBannerMessage = message,
            isDuplicateWarning = isWarning
        )
        bannerJob = viewModelScope.launch {
            delay(2500)
            _uiState.value = _uiState.value.copy(
                scanBannerMessage = null,
                isDuplicateWarning = false
            )
        }
    }

    fun scanImageFromGallery(context: Context, imageUri: Uri, onNavigateResult: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes.first()
                            val raw = barcode.rawValue ?: barcode.displayValue ?: ""
                            if (raw.isNotEmpty()) {
                                onBarcodeDetected(context, raw, "QR_CODE", onNavigateResult)
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(errorMessage = "No QR or barcode found in image.")
                        }
                    }
                    .addOnFailureListener {
                        _uiState.value = _uiState.value.copy(errorMessage = "Failed to process image: ${it.localizedMessage}")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error loading image: ${e.localizedMessage}")
            }
        }
    }

    fun resumeScanning() {
        _uiState.value = _uiState.value.copy(isScanningActive = true, errorMessage = null)
    }

    /**
     * Generates a CSV text representation of all items scanned in the current batch session.
     * Ordered chronologically (first scanned -> last scanned).
     */
    fun getBatchCsvExportText(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("No,Waktu,Format,Tipe,Judul,Konten\n")
        val items = _uiState.value.batchItems.reversed()
        items.forEachIndexed { index, item ->
            val dateStr = dateFormat.format(Date(item.scannedAt))
            val safeTitle = item.data.title.replace("\"", "\"\"")
            val safeContent = item.data.rawContent.replace("\"", "\"\"").replace("\n", " ")
            sb.append("${index + 1},\"$dateStr\",\"${item.data.format}\",\"${item.data.type}\",\"$safeTitle\",\"$safeContent\"\n")
        }
        return sb.toString()
    }

    /**
     * Generates a formatted text summary ideal for sharing via WhatsApp, Email, or Notes.
     */
    fun getBatchPlainTextSummary(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("📋 Laporan Sesi Pindai Batch - ScanFlow QR\n")
        sb.append("Total Item: ${_uiState.value.batchCount}\n")
        sb.append("Waktu Selesai: ${dateFormat.format(Date())}\n\n")
        val items = _uiState.value.batchItems.reversed()
        items.forEachIndexed { index, item ->
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.scannedAt))
            sb.append("${index + 1}. [$timeStr] [${item.data.type}] ${item.data.title}\n   ${item.data.rawContent}\n")
        }
        return sb.toString()
    }
}
