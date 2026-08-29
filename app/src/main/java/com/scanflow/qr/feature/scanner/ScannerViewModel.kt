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
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.ParseQrCodeUseCase
import com.scanflow.qr.domain.usecase.SaveScanResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isTorchEnabled: Boolean = false,
    val cameraLens: Int = CameraSelector.LENS_FACING_BACK,
    val isScanningActive: Boolean = true,
    val lastScannedId: Long? = null,
    val errorMessage: String? = null
)

class ScannerViewModel(
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val saveScanResultUseCase: SaveScanResultUseCase,
    private val getSettingsUseCase: GetSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScannedContent: String? = null
    private var lastScanTimestamp: Long = 0

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchEnabled = !_uiState.value.isTorchEnabled)
    }

    fun switchCamera() {
        val newLens = if (_uiState.value.cameraLens == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.value = _uiState.value.copy(cameraLens = newLens)
    }

    fun onBarcodeDetected(context: Context, rawValue: String, format: String, onNavigateResult: (Long) -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (rawValue == lastScannedContent && (currentTime - lastScanTimestamp) < 2000) {
            return // Prevent duplicate scan spam
        }

        if (!_uiState.value.isScanningActive) return

        lastScannedContent = rawValue
        lastScanTimestamp = currentTime
        _uiState.value = _uiState.value.copy(isScanningActive = false)

        viewModelScope.launch {
            val settings = getSettingsUseCase().first()
            if (settings.vibrateOnScan) VibratorHelper.vibrateSuccess(context)
            if (settings.beepOnScan) SoundHelper.playBeep()

            val parsedData = parseQrCodeUseCase(rawValue, format)
            val savedId = saveScanResultUseCase(parsedData)

            _uiState.value = _uiState.value.copy(lastScannedId = savedId)
            onNavigateResult(savedId)
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
}
