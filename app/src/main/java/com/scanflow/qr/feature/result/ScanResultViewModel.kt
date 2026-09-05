package com.scanflow.qr.feature.result

import android.content.Context
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

data class ScanResultUiState(
    val scanItem: ScanHistoryItem? = null,
    val parsedData: QrCodeData? = null,
    val securityAssessment: SecurityAssessment? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
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
                IntentHelper.saveContact(context, name, phone, email, org)
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
            QrType.WIFI -> IntentHelper.openWifiSettings(context)
            else -> ShareHelper.copyToClipboard(context, parsed.rawContent)
        }
    }

    fun copyContent(context: Context) {
        val parsed = _uiState.value.parsedData ?: return
        ShareHelper.copyToClipboard(context, parsed.rawContent)
    }

    fun shareContent(context: Context) {
        val parsed = _uiState.value.parsedData ?: return
        ShareHelper.shareText(context, parsed.rawContent, "Share Scanned QR Result")
    }
}
