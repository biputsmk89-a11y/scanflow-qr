package com.scanflow.qr.feature.generator

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
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
    val contactJobTitle: String = "",
    val contactWebsite: String = "",
    val contactAddress: String = "",
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
    // Barcode 1D
    val barcodeFormat: String = "CODE_128",
    val barcodeContent: String = "",
    // WhatsApp
    val whatsappCountryCode: String = "+62",
    val whatsappPhone: String = "",
    val whatsappMessage: String = "",

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
        
        // Validasi spesifik barcode
        if (state.selectedType == QrType.BARCODE) {
            val raw = state.barcodeContent.trim()
            if (raw.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Konten barcode tidak boleh kosong.")
                return
            }
            when (state.barcodeFormat) {
                "EAN_13", "EAN-13" -> {
                    val digits = raw.filter { it.isDigit() }
                    if (digits.length !in 12..13) {
                        _uiState.value = _uiState.value.copy(errorMessage = "Format EAN-13 harus berupa 12 atau 13 digit angka.")
                        return
                    }
                }
                "EAN_8", "EAN-8" -> {
                    val digits = raw.filter { it.isDigit() }
                    if (digits.length !in 7..8) {
                        _uiState.value = _uiState.value.copy(errorMessage = "Format EAN-8 harus berupa 7 atau 8 digit angka.")
                        return
                    }
                }
                "UPC_A", "UPC-A" -> {
                    val digits = raw.filter { it.isDigit() }
                    if (digits.length !in 11..12) {
                        _uiState.value = _uiState.value.copy(errorMessage = "Format UPC-A harus berupa 11 atau 12 digit angka.")
                        return
                    }
                }
            }
        }

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
                patternStyle = if (state.selectedType == QrType.BARCODE) state.barcodeFormat else "SQUARE",
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
                        if (state.contactJobTitle.isNotEmpty()) appendLine("TITLE:${state.contactJobTitle.trim()}")
                        if (state.contactWebsite.isNotEmpty()) appendLine("URL:${state.contactWebsite.trim()}")
                        if (state.contactAddress.isNotEmpty()) appendLine("ADR:;;${state.contactAddress.trim()};;;;")
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
            QrType.BARCODE -> {
                val raw = state.barcodeContent.trim()
                if (raw.isEmpty()) "" to ""
                else {
                    when (state.barcodeFormat) {
                        "EAN_13", "EAN-13" -> {
                            val digits = raw.filter { it.isDigit() }
                            if (digits.length in 12..13) {
                                digits to "EAN-13: $digits"
                            } else "" to ""
                        }
                        "EAN_8", "EAN-8" -> {
                            val digits = raw.filter { it.isDigit() }
                            if (digits.length in 7..8) {
                                digits to "EAN-8: $digits"
                            } else "" to ""
                        }
                        "UPC_A", "UPC-A" -> {
                            val digits = raw.filter { it.isDigit() }
                            if (digits.length in 11..12) {
                                digits to "UPC-A: $digits"
                            } else "" to ""
                        }
                        "CODE_39", "CODE-39" -> {
                            val clean = raw.uppercase()
                            clean to "Code 39: $clean"
                        }
                        else -> {
                            raw to "Barcode (Code 128): $raw"
                        }
                    }
                }
            }
            QrType.WHATSAPP -> {
                val cleanNumber = normalizeWhatsappNumber(state.whatsappCountryCode, state.whatsappPhone)
                if (cleanNumber.isEmpty()) "" to ""
                else {
                    val encodedMsg = encodeUriParam(state.whatsappMessage.trim())
                    val url = if (encodedMsg.isNotEmpty()) {
                        "https://wa.me/$cleanNumber?text=$encodedMsg"
                    } else {
                        "https://wa.me/$cleanNumber"
                    }
                    val title = "WhatsApp: +$cleanNumber"
                    url to title
                }
            }
        }
    }

    fun normalizeWhatsappNumber(countryCode: String, rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val codeDigits = countryCode.filter { it.isDigit() }

        return when {
            digits.startsWith(codeDigits) -> digits
            digits.startsWith("0") -> codeDigits + digits.substring(1)
            else -> codeDigits + digits
        }
    }

    fun importContactFromUri(context: Context, contactUri: Uri) {
        try {
            val contentResolver = context.contentResolver
            var name: String? = null
            var phone: String? = null
            var email: String? = null
            var org: String? = null
            var contactId: String? = null

            contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    if (idIndex != -1) {
                        contactId = cursor.getString(idIndex)
                    }
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }

            if (contactId != null) {
                // Query Phone
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        val numIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIndex != -1) {
                            phone = phoneCursor.getString(numIndex)
                        }
                    }
                }

                // Query Email
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { emailCursor ->
                    if (emailCursor.moveToFirst()) {
                        val mailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                        if (mailIndex != -1) {
                            email = emailCursor.getString(mailIndex)
                        }
                    }
                }

                // Query Organization
                contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                    null
                )?.use { orgCursor ->
                    if (orgCursor.moveToFirst()) {
                        val compIndex = orgCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                        if (compIndex != -1) {
                            org = orgCursor.getString(compIndex)
                        }
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                contactName = name ?: _uiState.value.contactName,
                contactPhone = phone ?: _uiState.value.contactPhone,
                contactEmail = email ?: _uiState.value.contactEmail,
                contactOrg = org ?: _uiState.value.contactOrg,
                errorMessage = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Gagal mengambil data kontak: ${e.localizedMessage}")
        }
    }

    fun importPhoneForWhatsapp(context: Context, contactUri: Uri) {
        try {
            val contentResolver = context.contentResolver
            var phone: String? = null
            var contactId: String? = null

            contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    if (idIndex != -1) {
                        contactId = cursor.getString(idIndex)
                    }
                }
            }

            if (contactId != null) {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        val numIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIndex != -1) {
                            phone = phoneCursor.getString(numIndex)
                        }
                    }
                }
            }

            if (!phone.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    whatsappPhone = phone ?: "",
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Gagal mengambil nomor kontak: ${e.localizedMessage}")
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
