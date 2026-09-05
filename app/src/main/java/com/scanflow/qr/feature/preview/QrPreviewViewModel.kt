package com.scanflow.qr.feature.preview

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.utils.QrCodeGenerator
import com.scanflow.qr.core.utils.ShareHelper
import com.scanflow.qr.domain.model.QrCornerStyle
import com.scanflow.qr.domain.model.QrExportFormat
import com.scanflow.qr.domain.model.QrPatternStyle
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QrPreviewUiState(
    val qrCode: UserQrCode? = null,
    val styleConfig: QrStyleConfig = QrStyleConfig(),
    val previewBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val isExportSheetVisible: Boolean = false,
    val exportSuccessMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class QrPreviewViewModel @Inject constructor(
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

    fun setExportSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isExportSheetVisible = visible)
    }

    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(exportSuccessMessage = null)
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

    /**
     * Exports the QR code to device storage in the specified format (PNG, SVG, or PDF).
     */
    fun exportQr(format: QrExportFormat) {
        val qr = _uiState.value.qrCode ?: return
        val config = _uiState.value.styleConfig

        viewModelScope.launch {
            when (format) {
                QrExportFormat.PNG -> {
                    val bitmap = _uiState.value.previewBitmap
                    if (bitmap == null) {
                        _uiState.value = _uiState.value.copy(errorMessage = "Gambar belum siap.")
                        return@launch
                    }
                    val uri = qrRepository.exportQrToGallery(bitmap, "ScanFlow_${qr.id}")
                    if (uri != null) {
                        qrRepository.incrementDownloadCount(qr.id)
                        _uiState.value = _uiState.value.copy(
                            exportSuccessMessage = "✅ Berhasil disimpan ke Galeri (PNG HD)"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(errorMessage = "Gagal menyimpan berkas PNG.")
                    }
                }

                QrExportFormat.SVG -> {
                    val svgString = qrRepository.generateQrSvg(qr.content, config)
                    if (svgString != null) {
                        val uri = qrRepository.exportQrSvg(svgString, "ScanFlow_Vector_${qr.id}")
                        if (uri != null) {
                            qrRepository.incrementDownloadCount(qr.id)
                            _uiState.value = _uiState.value.copy(
                                exportSuccessMessage = "✅ Berhasil disimpan ke Dokumen (SVG Vektor Murni)"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(errorMessage = "Gagal menyimpan berkas SVG.")
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(errorMessage = "Gagal merender vektor SVG.")
                    }
                }

                QrExportFormat.PDF -> {
                    val bitmap = _uiState.value.previewBitmap
                    val uri = qrRepository.exportQrPdf(
                        title = qr.title,
                        type = qr.type.displayName,
                        content = qr.content,
                        bitmap = bitmap,
                        filename = "ScanFlow_Doc_${qr.id}"
                    )
                    if (uri != null) {
                        qrRepository.incrementDownloadCount(qr.id)
                        _uiState.value = _uiState.value.copy(
                            exportSuccessMessage = "✅ Berhasil disimpan ke Dokumen (PDF Cetak A4)"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(errorMessage = "Gagal membuat berkas PDF.")
                    }
                }
            }
        }
    }

    /**
     * Caches and shares the QR code via Android Sharesheet in the specified format (PNG, SVG, or PDF).
     */
    fun shareQr(context: Context, format: QrExportFormat = QrExportFormat.PNG) {
        val qr = _uiState.value.qrCode ?: return
        val config = _uiState.value.styleConfig

        viewModelScope.launch {
            when (format) {
                QrExportFormat.PNG -> {
                    val bitmap = _uiState.value.previewBitmap ?: return@launch
                    val uri = qrRepository.cacheQrForSharing(bitmap, "shared_qr_${qr.id}.png")
                    if (uri != null) {
                        qrRepository.incrementShareCount(qr.id)
                        ShareHelper.shareFileUri(context, uri, format.mimeType, "Bagikan Kode QR (PNG)")
                    }
                }

                QrExportFormat.SVG -> {
                    val svgString = qrRepository.generateQrSvg(qr.content, config)
                    if (svgString != null) {
                        val uri = qrRepository.cacheQrSvgForSharing(svgString, "shared_vector_${qr.id}.svg")
                        if (uri != null) {
                            qrRepository.incrementShareCount(qr.id)
                            ShareHelper.shareFileUri(context, uri, format.mimeType, "Bagikan Vektor QR (SVG)")
                        }
                    }
                }

                QrExportFormat.PDF -> {
                    val bitmap = _uiState.value.previewBitmap
                    val uri = qrRepository.cacheQrPdfForSharing(
                        title = qr.title,
                        type = qr.type.displayName,
                        content = qr.content,
                        bitmap = bitmap,
                        filename = "shared_doc_${qr.id}.pdf"
                    )
                    if (uri != null) {
                        qrRepository.incrementShareCount(qr.id)
                        ShareHelper.shareFileUri(context, uri, format.mimeType, "Bagikan Dokumen Siap Cetak (PDF)")
                    }
                }
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
