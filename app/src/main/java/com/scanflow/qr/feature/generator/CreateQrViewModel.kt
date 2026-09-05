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
    val calendarDescription: String = "",
    val calendarDate: String = "",
    // Payment
    val paymentAddress: String = "",
    val paymentType: String = "UPI",
    val paymentPayeeName: String = "",
    val paymentAmount: String = "",
    val paymentReference: String = "",
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
                    val fullUrl = if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) "https://$url" else url
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
                    val safeSsid = escapeWifi(ssid)
                    val safePass = escapeWifi(state.wifiPassword.trim())
                    val secType = when (state.wifiSecurity) {
                        "WPA/WPA2", "WPA" -> "WPA"
                        "WPA3" -> "SAE"
                        "WEP" -> "WEP"
                        "nopass", "None" -> "nopass"
                        else -> state.wifiSecurity
                    }
                    val payload = "WIFI:T:$secType;S:$safeSsid;P:$safePass;H:${state.wifiHidden};;"
                    payload to "Wi-Fi: $ssid"
                }
            }
            QrType.CONTACT -> {
                val name = state.contactName.trim()
                if (name.isEmpty() && state.contactPhone.isEmpty()) "" to ""
                else {
                    val vcard = buildString {
                        appendLine("BEGIN:VCARD")
                        appendLine("VERSION:3.0")
                        if (name.isNotEmpty()) appendLine("FN:$name")
                        if (state.contactPhone.isNotEmpty()) appendLine("TEL:${state.contactPhone.trim()}")
                        if (state.contactEmail.isNotEmpty()) appendLine("EMAIL:${state.contactEmail.trim()}")
                        if (state.contactOrg.isNotEmpty()) appendLine("ORG:${state.contactOrg.trim()}")
                        append("END:VCARD")
                    }
                    vcard to (name.ifEmpty { "Contact Card" })
                }
            }
            QrType.EMAIL -> {
                val email = state.emailTo.trim()
                if (email.isEmpty()) "" to ""
                else {
                    val encSubject = encodeUriParam(state.emailSubject.trim())
                    val encBody = encodeUriParam(state.emailBody.trim())
                    val queryParams = mutableListOf<String>()
                    if (encSubject.isNotEmpty()) queryParams.add("subject=$encSubject")
                    if (encBody.isNotEmpty()) queryParams.add("body=$encBody")
                    val query = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
                    val payload = "mailto:$email$query"
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
                else "smsto:$phone:${state.smsMessage.trim()}" to "SMS to $phone"
            }
            QrType.LOCATION -> {
                val lat = state.locationLat.trim()
                val lng = state.locationLng.trim()
                if (lat.isEmpty() || lng.isEmpty()) "" to ""
                else {
                    val latNum = lat.toDoubleOrNull()
                    val lngNum = lng.toDoubleOrNull()
                    if (latNum == null || latNum < -90.0 || latNum > 90.0 ||
                        lngNum == null || lngNum < -180.0 || lngNum > 180.0) {
                        "" to ""
                    } else {
                        "geo:$lat,$lng" to "Location: $lat, $lng"
                    }
                }
            }
            QrType.CALENDAR -> {
                val title = state.calendarTitle.trim()
                if (title.isEmpty()) "" to ""
                else {
                    val vEvent = buildString {
                        appendLine("BEGIN:VEVENT")
                        appendLine("SUMMARY:$title")
                        if (state.calendarLocation.isNotEmpty()) appendLine("LOCATION:${state.calendarLocation.trim()}")
                        if (state.calendarDescription.isNotEmpty()) appendLine("DESCRIPTION:${state.calendarDescription.trim()}")
                        if (state.calendarDate.isNotEmpty()) {
                            val cleanDate = state.calendarDate.replace("-", "").replace(":", "").trim()
                            appendLine("DTSTART:$cleanDate")
                        }
                        append("END:VEVENT")
                    }
                    vEvent to title
                }
            }
            QrType.PAYMENT -> {
                val addr = state.paymentAddress.trim()
                val payee = state.paymentPayeeName.trim()
                val amount = state.paymentAmount.trim()
                val ref = state.paymentReference.trim()

                if (addr.isEmpty() && payee.isEmpty()) "" to ""
                else {
                    val payload = when {
                        addr.startsWith("http://", ignoreCase = true) || addr.startsWith("https://", ignoreCase = true) -> addr
                        state.paymentType == "UPI" -> {
                            val pa = if (addr.isNotEmpty()) addr else "merchant@upi"
                            val pn = if (payee.isNotEmpty()) "&pn=${encodeUriParam(payee)}" else ""
                            val am = if (amount.isNotEmpty()) "&am=$amount" else ""
                            val tr = if (ref.isNotEmpty()) "&tr=${encodeUriParam(ref)}" else ""
                            "upi://pay?pa=$pa$pn$am$tr"
                        }
                        state.paymentType == "Bitcoin" -> {
                            val am = if (amount.isNotEmpty()) "?amount=$amount" else ""
                            "bitcoin:$addr$am"
                        }
                        state.paymentType == "Ethereum" -> {
                            val am = if (amount.isNotEmpty()) "?value=$amount" else ""
                            "ethereum:$addr$am"
                        }
                        else -> {
                            if (addr.isNotEmpty()) "https://paypal.me/$addr" else "https://pay.stripe.com"
                        }
                    }
                    val title = if (payee.isNotEmpty()) {
                        if (amount.isNotEmpty()) "Payment to $payee ($amount)" else "Payment to $payee"
                    } else {
                        "Payment QR"
                    }
                    payload to title
                }
            }
            QrType.SOCIAL -> {
                val rawUser = state.socialUsername.trim()
                val user = rawUser.removePrefix("@").trim()
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

    private fun escapeWifi(value: String): String {
        return value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(":", "\\:")
            .replace(",", "\\,")
    }

    private fun encodeUriParam(value: String): String {
        return try {
            java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        } catch (e: Exception) {
            value
        }
    }
}
