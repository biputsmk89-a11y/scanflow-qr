package com.scanflow.qr.feature.preview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.utils.QrCodeGenerator
import com.scanflow.qr.core.utils.ShareHelper
import com.scanflow.qr.domain.model.QrCornerStyle
import com.scanflow.qr.domain.model.QrPatternStyle
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QrPreviewUiState(
    val qrCode: UserQrCode? = null,
    val styleConfig: QrStyleConfig = QrStyleConfig(),
    val previewBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val exportSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class QrPreviewViewModel(
    private val qrRepository: QrGeneratorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrPreviewUiState())
    val uiState: StateFlow<QrPreviewUiState> = _uiState.asStateFlow()

    fun loadQr(qrId: Long) {
        viewModelScope.launch {
            val qr = qrRepository.getUserQrById(qrId)
            if (qr != null) {
                val config = QrStyleConfig(
                    foregroundColor = qr.foregroundColor,
                    backgroundColor = qr.backgroundColor,
                    patternStyle = QrPatternStyle.valueOf(qr.patternStyle.ifEmpty { "SQUARE" }),
                    cornerEyeStyle = QrCornerStyle.valueOf(qr.eyeStyle.ifEmpty { "SQUARE" })
                )
                val bitmap = QrCodeGenerator.generateQrBitmap(qr.content, config)
                _uiState.value = QrPreviewUiState(
                    qrCode = qr,
                    styleConfig = config,
                    previewBitmap = bitmap,
                    isLoading = false
                )
            } else {
                _uiState.value = QrPreviewUiState(isLoading = false)
            }
        }
    }

    fun updateForegroundColor(color: Int) {
        val newConfig = _uiState.value.styleConfig.copy(foregroundColor = color)
        applyNewConfig(newConfig)
    }

    fun updateBackgroundColor(color: Int) {
        val newConfig = _uiState.value.styleConfig.copy(backgroundColor = color)
        applyNewConfig(newConfig)
    }

    fun updatePatternStyle(style: QrPatternStyle) {
        val newConfig = _uiState.value.styleConfig.copy(patternStyle = style)
        applyNewConfig(newConfig)
    }

    fun updateEyeStyle(style: QrCornerStyle) {
        val newConfig = _uiState.value.styleConfig.copy(cornerEyeStyle = style)
        applyNewConfig(newConfig)
    }

    private fun applyNewConfig(newConfig: QrStyleConfig) {
        val content = _uiState.value.qrCode?.content ?: return
        val bitmap = QrCodeGenerator.generateQrBitmap(content, newConfig)
        _uiState.value = _uiState.value.copy(
            styleConfig = newConfig,
            previewBitmap = bitmap
        )
    }

    fun exportToGallery(context: Context) {
        val bitmap = _uiState.value.previewBitmap ?: return
        val qr = _uiState.value.qrCode ?: return

        viewModelScope.launch {
            val uri = qrRepository.exportQrToGallery(bitmap, "ScanFlow_${qr.id}")
            if (uri != null) {
                qrRepository.incrementDownloadCount(qr.id)
                _uiState.value = _uiState.value.copy(exportSuccessMessage = "Saved to Gallery (Pictures/ScanFlowQR)")
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to export image.")
            }
        }
    }

    fun shareQr(context: Context) {
        val bitmap = _uiState.value.previewBitmap ?: return
        val qr = _uiState.value.qrCode ?: return

        viewModelScope.launch {
            val uri = qrRepository.cacheQrForSharing(bitmap, "shared_qr_${qr.id}.png")
            if (uri != null) {
                qrRepository.incrementShareCount(qr.id)
                ShareHelper.shareImageUri(context, uri, "Share QR Code")
            }
        }
    }

    fun toggleFavorite() {
        val qr = _uiState.value.qrCode ?: return
        viewModelScope.launch {
            val newFav = !qr.isFavorite
            qrRepository.toggleFavorite(qr.id, newFav)
            _uiState.value = _uiState.value.copy(qrCode = qr.copy(isFavorite = newFav))
        }
    }
}
