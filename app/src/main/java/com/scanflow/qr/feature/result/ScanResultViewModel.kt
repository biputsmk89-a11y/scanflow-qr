package com.scanflow.qr.feature.result

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.security.UrlSecurityChecker
import com.scanflow.qr.core.utils.IntentHelper
import com.scanflow.qr.core.utils.QrCodeParser
import com.scanflow.qr.core.utils.ShareHelper
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.SecurityAssessment
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.scanflow.qr.core.utils.WifiConnectionStatus
import com.scanflow.qr.core.utils.WifiConnector

data class ScanResultUiState(
    val scanItem: ScanHistoryItem? = null,
    val parsedData: QrCodeData? = null,
    val securityAssessment: SecurityAssessment? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val wifiConnectionStatus: WifiConnectionStatus = WifiConnectionStatus.IDLE,
    val wifiConnectionMessage: String? = null
)

@HiltViewModel
class ScanResultViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanResultUiState())
    val uiState: StateFlow<ScanResultUiState> = _uiState.asStateFlow()

    fun loadScanResult(scanId: Long) {
        viewModelScope.launch {
            val item = historyRepository.getHistoryItemById(scanId)
            if (item != null) {
                val parsed = QrCodeParser.parse(item.content, item.format)
                val security = if (parsed.type == QrType.WEBSITE || parsed.type == QrType.SOCIAL) {
                    UrlSecurityChecker.assessUrl(parsed.rawContent)
                } else null

                _uiState.value = ScanResultUiState(
                    scanItem = item,
                    parsedData = parsed,
                    securityAssessment = security,
                    isLoading = false
                )
            } else {
                _uiState.value = ScanResultUiState(isLoading = false)
            }
        }
    }

    fun toggleFavorite() {
        val currentItem = _uiState.value.scanItem ?: return
        viewModelScope.launch {
            val newFav = !currentItem.isFavorite
            toggleFavoriteUseCase.toggleScanFavorite(currentItem.id, newFav)
            _uiState.value = _uiState.value.copy(
                scanItem = currentItem.copy(isFavorite = newFav)
            )
        }
    }

    fun deleteScan(onDeleted: () -> Unit) {
        val currentItem = _uiState.value.scanItem ?: return
        viewModelScope.launch {
            historyRepository.deleteHistoryItem(currentItem.id)
            _uiState.value = _uiState.value.copy(isDeleted = true)
            onDeleted()
        }
    }

    fun performPrimaryAction(context: Context) {
        val parsed = _uiState.value.parsedData ?: return
        when (parsed.type) {
            QrType.WEBSITE, QrType.SOCIAL -> IntentHelper.openUrl(context, parsed.rawContent)
            QrType.PHONE -> {
                val phone = parsed.displayDetails["Phone Number"] ?: parsed.rawContent.removePrefix("tel:")
                IntentHelper.callPhone(context, phone)
            }
            QrType.EMAIL -> {
                val email = parsed.displayDetails["Recipient"] ?: parsed.rawContent.removePrefix("mailto:")
                val subject = parsed.displayDetails["Subject"] ?: ""
                val body = parsed.displayDetails["Message"] ?: ""
                IntentHelper.sendEmail(context, email, subject, body)
            }
            QrType.SMS -> {
                val phone = parsed.displayDetails["Phone Number"] ?: ""
                val msg = parsed.displayDetails["Message"] ?: ""
                IntentHelper.sendSms(context, phone, msg)
            }
            QrType.LOCATION -> {
                val lat = parsed.displayDetails["Latitude"] ?: "0"
                val lng = parsed.displayDetails["Longitude"] ?: "0"
                IntentHelper.openMapLocation(context, lat, lng)
            }
            QrType.CONTACT -> {
                val name = parsed.displayDetails["Name"] ?: ""
                val phone = parsed.displayDetails["Phone"] ?: ""
                val email = parsed.displayDetails["Email"] ?: ""
                val org = parsed.displayDetails["Organization"] ?: ""
                val jobTitle = parsed.displayDetails["Job Title"] ?: ""
                val address = parsed.displayDetails["Address"] ?: ""
                IntentHelper.saveContact(context, name, phone, email, org, jobTitle, address)
            }
            QrType.CALENDAR -> {
                val title = parsed.displayDetails["Event Title"] ?: parsed.title
                val location = parsed.displayDetails["Location"] ?: ""
                val desc = parsed.displayDetails["Description"] ?: ""
                IntentHelper.addCalendarEvent(context, title, location, desc)
            }
            QrType.PAYMENT -> {
                IntentHelper.openPayment(context, parsed.rawContent)
            }
            QrType.WIFI -> {
                connectWifi(context)
            }
            QrType.BARCODE -> {
                searchProductGoogle(context, parsed.rawContent)
            }
            QrType.WHATSAPP -> {
                IntentHelper.openUrl(context, parsed.rawContent)
            }
            else -> ShareHelper.copyToClipboard(context, parsed.rawContent)
        }
    }

    fun searchProductGoogle(context: Context, query: String = _uiState.value.parsedData?.rawContent.orEmpty()) {
        if (query.isNotBlank()) {
            IntentHelper.searchProductGoogle(context, query)
        }
    }

    fun searchProductBarcodeLookup(context: Context, query: String = _uiState.value.parsedData?.rawContent.orEmpty()) {
        if (query.isNotBlank()) {
            IntentHelper.searchProductBarcodeLookup(context, query)
        }
    }

    fun searchProductOpenFoodFacts(context: Context, query: String = _uiState.value.parsedData?.rawContent.orEmpty()) {
        if (query.isNotBlank()) {
            IntentHelper.searchOpenFoodFacts(context, query)
        }
    }

    fun copyPartialContent(context: Context, label: String, value: String) {
        ShareHelper.copyToClipboard(context, value)
        Toast.makeText(context, "$label disalin ke papan klip", Toast.LENGTH_SHORT).show()
    }

    fun connectWifi(context: Context) {
        val parsed = _uiState.value.parsedData ?: return
        val ssid = parsed.displayDetails["Network (SSID)"] ?: ""
        val password = parsed.displayDetails["Password"]
        val security = parsed.displayDetails["Security"] ?: "WPA"
        val isHidden = parsed.displayDetails["Hidden Network"]?.equals("Yes", ignoreCase = true) ?: false

        if (ssid.isBlank()) {
            _uiState.value = _uiState.value.copy(
                wifiConnectionStatus = WifiConnectionStatus.FAILED,
                wifiConnectionMessage = "Nama jaringan Wi-Fi (SSID) kosong."
            )
            return
        }

        // Salin password ke clipboard sebagai cadangan bantuan untuk pengguna
        if (!password.isNullOrEmpty()) {
            ShareHelper.copyToClipboard(context, password)
        }

        WifiConnector.connectToWifi(
            context = context,
            ssid = ssid,
            password = password,
            securityType = security,
            isHidden = isHidden
        ) { status, message ->
            _uiState.value = _uiState.value.copy(
                wifiConnectionStatus = status,
                wifiConnectionMessage = message
            )
            if (status == WifiConnectionStatus.CONNECTED || status == WifiConnectionStatus.FAILED) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openWifiSettings(context: Context) {
        IntentHelper.openWifiSettings(context)
    }

    override fun onCleared() {
        super.onCleared()
        WifiConnector.cancelCurrentConnection()
    }

    fun copyContent(context: Context) {
        val parsed = _uiState.value.parsedData ?: return
        ShareHelper.copyToClipboard(context, parsed.rawContent)
    }

    fun shareContent(context: Context) {
        val parsed = _uiState.value.parsedData ?: return
        ShareHelper.shareText(context, parsed.rawContent, "Share Scanned QR Result")
    }

    /**
     * Mencetak barcode atau QR code hasil pemindaian langsung ke printer sistem.
     */
    fun printScanResult(context: Context) {
        val item = _uiState.value.scanItem ?: return
        val parsed = _uiState.value.parsedData
        val is1DBarcode = parsed?.type == QrType.BARCODE || item.format in listOf("EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_128", "CODE_39", "CODE_93", "ITF", "CODABAR")

        val bitmap = if (is1DBarcode) {
            com.scanflow.qr.core.utils.QrCodeGenerator.generateBarcodeBitmap(
                content = item.content,
                formatName = item.format.ifEmpty { "CODE_128" }
            ) ?: com.scanflow.qr.core.utils.QrCodeGenerator.generateQrBitmap(
                content = item.content,
                config = com.scanflow.qr.domain.model.QrStyleConfig()
            )
        } else {
            com.scanflow.qr.core.utils.QrCodeGenerator.generateQrBitmap(
                content = item.content,
                config = com.scanflow.qr.domain.model.QrStyleConfig()
            )
        }

        if (bitmap == null) {
            Toast.makeText(context, "Gagal memproses kode untuk dicetak", Toast.LENGTH_SHORT).show()
            return
        }

        com.scanflow.qr.core.utils.AppPrintHelper.printQrBitmap(
            context = context,
            jobName = item.title.ifEmpty { "Scan_${item.id}" },
            bitmap = bitmap,
            title = item.title.ifEmpty { "Hasil Pemindaian" },
            subtitle = if (is1DBarcode) "Barcode (${item.format})" else parsed?.type?.displayName ?: item.format,
            content = item.content
        )
    }
}
