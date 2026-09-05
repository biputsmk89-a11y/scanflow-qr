package com.scanflow.qr.feature.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateQrUiState(
    val selectedType: QrType = QrType.WEBSITE,
    // Website / Text
    val textOrUrl: String = "",
    // WiFi
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: String = "WPA",
    val wifiHidden: Boolean = false,
    // Contact
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrg: String = "",
    // Email
    val emailTo: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    // Phone
    val phoneNumber: String = "",
    // SMS
    val smsPhone: String = "",
    val smsMessage: String = "",
    // Location
    val locationLat: String = "",
    val locationLng: String = "",
    // Calendar
    val calendarTitle: String = "",
    val calendarLocation: String = "",
    // Payment
    val paymentAddress: String = "",
    val paymentType: String = "UPI",
    // Social
    val socialPlatform: String = "Instagram",
    val socialUsername: String = "",

    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateQrViewModel @Inject constructor(
    private val qrRepository: QrGeneratorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQrUiState())
    val uiState: StateFlow<CreateQrUiState> = _uiState.asStateFlow()

    fun selectType(type: QrType) {
        _uiState.value = _uiState.value.copy(selectedType = type, errorMessage = null)
    }

    fun updateField(block: CreateQrUiState.() -> CreateQrUiState) {
        _uiState.value = _uiState.value.block()
    }

    fun generateAndSaveQr(onSuccess: (Long) -> Unit) {
        val state = _uiState.value
        val (formattedContent, title) = buildPayload(state)

        if (formattedContent.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in the required fields.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val userQr = UserQrCode(
                type = state.selectedType,
                title = title,
                content = formattedContent,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val savedId = qrRepository.saveUserQr(userQr)
            _uiState.value = _uiState.value.copy(isGenerating = false)
            onSuccess(savedId)
        }
    }

    private fun buildPayload(state: CreateQrUiState): Pair<String, String> {
        return when (state.selectedType) {
            QrType.WEBSITE -> {
                val url = state.textOrUrl.trim()
                if (url.isEmpty()) "" to ""
                else {
                    val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                    fullUrl to fullUrl
                }
            }
            QrType.TEXT -> {
                val text = state.textOrUrl.trim()
                val title = if (text.length > 25) text.take(25) + "..." else text
                text to title
            }
            QrType.WIFI -> {
                val ssid = state.wifiSsid.trim()
                if (ssid.isEmpty()) "" to ""
                else {
                    val payload = "WIFI:T:${state.wifiSecurity};S:$ssid;P:${state.wifiPassword};H:${state.wifiHidden};;"
                    payload to "Wi-Fi: $ssid"
                }
            }
            QrType.CONTACT -> {
                val name = state.contactName.trim()
                if (name.isEmpty() && state.contactPhone.isEmpty()) "" to ""
                else {
                    val vcard = """
                        BEGIN:VCARD
                        VERSION:3.0
                        FN:$name
                        TEL:${state.contactPhone.trim()}
                        EMAIL:${state.contactEmail.trim()}
                        ORG:${state.contactOrg.trim()}
                        END:VCARD
                    """.trimIndent()
                    vcard to (name.ifEmpty { "Contact Card" })
                }
            }
            QrType.EMAIL -> {
                val email = state.emailTo.trim()
                if (email.isEmpty()) "" to ""
                else {
                    val payload = "mailto:$email?subject=${state.emailSubject}&body=${state.emailBody}"
                    payload to "Email: $email"
                }
            }
            QrType.PHONE -> {
                val phone = state.phoneNumber.trim()
                if (phone.isEmpty()) "" to ""
                else "tel:$phone" to "Phone: $phone"
            }
            QrType.SMS -> {
                val phone = state.smsPhone.trim()
                if (phone.isEmpty()) "" to ""
                else "smsto:$phone:${state.smsMessage}" to "SMS to $phone"
            }
            QrType.LOCATION -> {
                val lat = state.locationLat.trim()
                val lng = state.locationLng.trim()
                if (lat.isEmpty() || lng.isEmpty()) "" to ""
                else "geo:$lat,$lng" to "Location: $lat, $lng"
            }
            QrType.CALENDAR -> {
                val title = state.calendarTitle.trim()
                if (title.isEmpty()) "" to ""
                else {
                    val vEvent = """
                        BEGIN:VEVENT
                        SUMMARY:$title
                        LOCATION:${state.calendarLocation.trim()}
                        END:VEVENT
                    """.trimIndent()
                    vEvent to title
                }
            }
            QrType.PAYMENT -> {
                val addr = state.paymentAddress.trim()
                if (addr.isEmpty()) "" to ""
                else {
                    val payload = when (state.paymentType) {
                        "UPI" -> "upi://pay?pa=$addr"
                        "Bitcoin" -> "bitcoin:$addr"
                        "Ethereum" -> "ethereum:$addr"
                        else -> "https://paypal.me/$addr"
                    }
                    payload to "${state.paymentType} Payment"
                }
            }
            QrType.SOCIAL -> {
                val user = state.socialUsername.trim()
                if (user.isEmpty()) "" to ""
                else {
                    val url = when (state.socialPlatform) {
                        "Instagram" -> "https://instagram.com/$user"
                        "Twitter / X" -> "https://x.com/$user"
                        "TikTok" -> "https://tiktok.com/@$user"
                        "LinkedIn" -> "https://linkedin.com/in/$user"
                        "GitHub" -> "https://github.com/$user"
                        else -> "https://youtube.com/@$user"
                    }
                    url to "${state.socialPlatform}: @$user"
                }
            }
            QrType.BARCODE -> "" to ""
        }
    }
}
