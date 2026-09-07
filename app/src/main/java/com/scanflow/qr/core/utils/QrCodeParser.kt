package com.scanflow.qr.core.utils

import com.scanflow.qr.core.security.UrlSecurityChecker
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrType

object QrCodeParser {

    fun parse(rawContent: String, format: String = "QR_CODE"): QrCodeData {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            return QrCodeData(
                rawContent = rawContent,
                type = QrType.TEXT,
                title = "Empty Content",
                format = format,
                isSecure = true
            )
        }

        // Check Barcode formats
        if (format != "QR_CODE" && format != "UNKNOWN" && format.isNotEmpty()) {
            val gs1Info = Gs1BarcodeParser.parse(trimmed, format)
            val details = mutableMapOf<String, String>()
            details["Code"] = trimmed
            details["Symbology"] = format
            if (gs1Info != null) {
                details["Country / Origin"] = "${gs1Info.flagEmoji} ${gs1Info.countryOrType}"
                details["GS1 Prefix"] = gs1Info.prefix
                if (gs1Info.isChecksumValid != null) {
                    details["Checksum Status"] = if (gs1Info.isChecksumValid) "Valid (Modulo-10 ✓)" else "Invalid Check Digit ⚠️"
                }
            }

            val title = if (gs1Info != null) {
                "${gs1Info.flagEmoji} ${gs1Info.countryOrType} ($format)"
            } else {
                "Barcode ($format)"
            }

            return QrCodeData(
                rawContent = trimmed,
                type = QrType.BARCODE,
                title = title,
                displayDetails = details,
                format = format,
                isSecure = gs1Info?.isChecksumValid != false,
                securityWarning = if (gs1Info?.isChecksumValid == false) "Checksum Modulo-10 barcode ini tidak valid atau barcode rusak." else null
            )
        }

        // 1. Wi-Fi: WIFI:S:MySSID;T:WPA;P:MyPassword;;
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            return parseWifi(trimmed, format)
        }

        // 2. Contact: vCard or MeCard
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true) || trimmed.startsWith("MECARD:", ignoreCase = true)) {
            return parseContact(trimmed, format)
        }

        // 3. Calendar: vEvent
        if (trimmed.contains("BEGIN:VEVENT", ignoreCase = true)) {
            return parseCalendar(trimmed, format)
        }

        // 4. Email: mailto: or MATMSG:
        if (trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            return parseEmail(trimmed, format)
        }

        // 5. Phone: tel:
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val phone = trimmed.substring(4)
            return QrCodeData(
                rawContent = trimmed,
                type = QrType.PHONE,
                title = phone,
                displayDetails = mapOf("Phone Number" to phone),
                format = format,
                isSecure = true
            )
        }

        // 6. SMS: sms: or SMSTO:
        if (trimmed.startsWith("sms:", ignoreCase = true) || trimmed.startsWith("smsto:", ignoreCase = true)) {
            return parseSms(trimmed, format)
        }

        // 7. Geo Location: geo:lat,lng
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            return parseGeo(trimmed, format)
        }

        // 8. Payment & Crypto: upi://, bitcoin:, ethereum:, etc.
        if (trimmed.startsWith("upi://", ignoreCase = true) ||
            trimmed.startsWith("bitcoin:", ignoreCase = true) ||
            trimmed.startsWith("ethereum:", ignoreCase = true) ||
            trimmed.startsWith("solana:", ignoreCase = true) ||
            trimmed.contains("paypal.me", ignoreCase = true)
        ) {
            return parsePayment(trimmed, format)
        }

        // WhatsApp Chat: https://wa.me/... or https://api.whatsapp.com/send...
        if (trimmed.contains("wa.me/", ignoreCase = true) || trimmed.contains("api.whatsapp.com/send", ignoreCase = true)) {
            return parseWhatsapp(trimmed, format)
        }

        // 9. Website URL or Social Links
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true) ||
            (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.matches(Regex("""^[a-zA-Z0-9-]+\.[a-zA-Z]{2,}(/.*)?$""")))
        ) {
            val url = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                "https://$trimmed"
            } else trimmed

            val assessment = UrlSecurityChecker.assessUrl(url)
            val isSocial = isSocialUrl(url)

            return QrCodeData(
                rawContent = url,
                type = if (isSocial) QrType.SOCIAL else QrType.WEBSITE,
                title = assessment.domain ?: url,
                displayDetails = mapOf(
                    "URL" to url,
                    "Security Status" to assessment.summary,
                    "Protocol" to if (assessment.isHttps) "HTTPS (Encrypted)" else "HTTP (Unencrypted)"
                ),
                format = format,
                isSecure = assessment.isSecure,
                securityWarning = if (!assessment.isSecure) assessment.summary else null
            )
        }

        // 10. Default Plain Text
        return QrCodeData(
            rawContent = trimmed,
            type = QrType.TEXT,
            title = if (trimmed.length > 30) trimmed.take(30) + "..." else trimmed,
            displayDetails = mapOf("Characters" to "${trimmed.length}"),
            format = format,
            isSecure = true
        )
    }

    private fun parseWifi(content: String, format: String): QrCodeData {
        var ssid = ""
        var password = ""
        var type = "WPA"
        var hidden = false

        // Remove prefix and trailing delimiters safely
        val body = content.removePrefix("WIFI:").removePrefix("wifi:")
            .removeSuffix(";;").removeSuffix(";")
        // Split by semicolon not preceded by an escape backslash
        val parts = body.split(Regex("(?<!\\\\);"))
        for (part in parts) {
            when {
                part.startsWith("S:") -> ssid = unescapeWifi(part.substring(2))
                part.startsWith("P:") -> password = unescapeWifi(part.substring(2))
                part.startsWith("T:") -> type = part.substring(2)
                part.startsWith("H:") -> hidden = part.substring(2).equals("true", ignoreCase = true)
            }
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.WIFI,
            title = if (ssid.isNotEmpty()) "Wi-Fi: $ssid" else "Wi-Fi Network",
            displayDetails = buildMap {
                put("Network (SSID)", ssid)
                if (password.isNotEmpty()) put("Password", password)
                put("Security", type.ifEmpty { "Open / None" })
                put("Hidden Network", if (hidden) "Yes" else "No")
            },
            format = format,
            isSecure = true
        )
    }

    private fun unescapeWifi(value: String): String {
        return value.replace("\\;", ";")
            .replace("\\:", ":")
            .replace("\\\\", "\\")
            .replace("\\,", ",")
    }

    private fun parseContact(content: String, format: String): QrCodeData {
        var name = ""
        var phone = ""
        var email = ""
        var org = ""
        var jobTitle = ""
        var website = ""
        var address = ""

        val lines = content.lines()
        for (line in lines) {
            val upper = line.uppercase()
            when {
                upper.startsWith("FN:") -> name = line.substring(3).trim()
                upper.startsWith("N:") && name.isEmpty() -> name = line.substring(2).replace(";", " ").trim()
                upper.startsWith("TEL:") || upper.startsWith("TEL;") -> phone = line.substringAfter(":").trim()
                upper.startsWith("EMAIL:") || upper.startsWith("EMAIL;") -> email = line.substringAfter(":").trim()
                upper.startsWith("ORG:") -> org = line.substring(4).trim()
                upper.startsWith("TITLE:") -> jobTitle = line.substring(6).trim()
                upper.startsWith("URL:") -> website = line.substring(4).trim()
                upper.startsWith("ADR:") || upper.startsWith("ADR;") -> {
                    address = line.substringAfter(":").replace(";", " ").trim()
                }
            }
        }

        // MeCard format fallback
        if (content.startsWith("MECARD:", ignoreCase = true)) {
            val parts = content.substring(7).split(";")
            for (part in parts) {
                when {
                    part.startsWith("N:") -> name = part.substring(2)
                    part.startsWith("TEL:") -> phone = part.substring(4)
                    part.startsWith("EMAIL:") -> email = part.substring(6)
                    part.startsWith("ORG:") -> org = part.substring(4)
                    part.startsWith("TITLE:") -> jobTitle = part.substring(6)
                    part.startsWith("URL:") -> website = part.substring(4)
                    part.startsWith("ADR:") -> address = part.substring(4).replace(";", " ").trim()
                }
            }
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.CONTACT,
            title = name.ifEmpty { "Contact Card" },
            displayDetails = buildMap {
                if (name.isNotEmpty()) put("Name", name)
                if (phone.isNotEmpty()) put("Phone", phone)
                if (email.isNotEmpty()) put("Email", email)
                if (org.isNotEmpty()) put("Organization", org)
                if (jobTitle.isNotEmpty()) put("Job Title", jobTitle)
                if (website.isNotEmpty()) put("Website", website)
                if (address.isNotEmpty()) put("Address", address)
            },
            format = format,
            isSecure = true
        )
    }

    private fun parseEmail(content: String, format: String): QrCodeData {
        var email = ""
        var subject = ""
        var body = ""

        if (content.startsWith("mailto:", ignoreCase = true)) {
            val uriStr = content.substring(7)
            email = uriStr.substringBefore("?")
            if (uriStr.contains("?")) {
                val query = uriStr.substringAfter("?")
                val params = query.split("&")
                for (param in params) {
                    if (param.startsWith("subject=", ignoreCase = true)) {
                        subject = decodeUrlComponent(param.substring(8))
                    }
                    if (param.startsWith("body=", ignoreCase = true)) {
                        body = decodeUrlComponent(param.substring(5))
                    }
                }
            }
        } else if (content.startsWith("MATMSG:", ignoreCase = true)) {
            val parts = content.substring(7).split(";")
            for (part in parts) {
                if (part.startsWith("TO:", ignoreCase = true)) email = part.substring(3)
                if (part.startsWith("SUB:", ignoreCase = true)) subject = part.substring(4)
                if (part.startsWith("BODY:", ignoreCase = true)) body = part.substring(5)
            }
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.EMAIL,
            title = email.ifEmpty { "Email Message" },
            displayDetails = buildMap {
                put("Recipient", email)
                if (subject.isNotEmpty()) put("Subject", subject)
                if (body.isNotEmpty()) put("Message", body)
            },
            format = format,
            isSecure = true
        )
    }

    private fun parseSms(content: String, format: String): QrCodeData {
        var phone = ""
        var message = ""

        if (content.startsWith("sms:", ignoreCase = true)) {
            val data = content.substring(4)
            phone = data.substringBefore("?")
            if (data.contains("body=", ignoreCase = true)) {
                message = decodeUrlComponent(data.substringAfter("body="))
            }
        } else if (content.startsWith("smsto:", ignoreCase = true)) {
            val data = content.substring(6)
            phone = data.substringBefore(":")
            message = data.substringAfter(":", "")
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.SMS,
            title = if (phone.isNotEmpty()) "SMS to $phone" else "SMS Message",
            displayDetails = buildMap {
                if (phone.isNotEmpty()) put("Phone Number", phone)
                if (message.isNotEmpty()) put("Message", message)
            },
            format = format,
            isSecure = true
        )
    }

    private fun parseGeo(content: String, format: String): QrCodeData {
        val coords = content.substring(4).substringBefore("?")
        val parts = coords.split(",")
        val lat = parts.getOrNull(0)?.trim() ?: "0.0"
        val lng = parts.getOrNull(1)?.trim() ?: "0.0"

        return QrCodeData(
            rawContent = content,
            type = QrType.LOCATION,
            title = "Location: $lat, $lng",
            displayDetails = mapOf("Latitude" to lat, "Longitude" to lng),
            format = format,
            isSecure = true
        )
    }

    private fun parseCalendar(content: String, format: String): QrCodeData {
        var title = "Calendar Event"
        var location = ""
        var description = ""
        var dtStart = ""
        var dtEnd = ""

        val lines = content.lines()
        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("SUMMARY:", ignoreCase = true) -> {
                    title = trimmedLine.substring(8).trim()
                }
                trimmedLine.startsWith("LOCATION:", ignoreCase = true) -> {
                    location = trimmedLine.substring(9).trim()
                }
                trimmedLine.startsWith("DESCRIPTION:", ignoreCase = true) -> {
                    description = trimmedLine.substring(12).trim()
                }
                trimmedLine.startsWith("DTSTART:", ignoreCase = true) || trimmedLine.startsWith("DTSTART;") -> {
                    dtStart = trimmedLine.substringAfter(":").trim()
                }
                trimmedLine.startsWith("DTEND:", ignoreCase = true) || trimmedLine.startsWith("DTEND;") -> {
                    dtEnd = trimmedLine.substringAfter(":").trim()
                }
            }
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.CALENDAR,
            title = title,
            displayDetails = buildMap {
                put("Event Title", title)
                if (location.isNotEmpty()) put("Location", location)
                if (description.isNotEmpty()) put("Description", description)
                if (dtStart.isNotEmpty()) put("Start Time", dtStart)
                if (dtEnd.isNotEmpty()) put("End Time", dtEnd)
            },
            format = format,
            isSecure = true
        )
    }

    private fun parsePayment(content: String, format: String): QrCodeData {
        val paymentType: String
        val details = mutableMapOf<String, String>()

        when {
            content.startsWith("upi://", ignoreCase = true) -> {
                paymentType = "UPI Payment"
                details["Type"] = paymentType
                val query = content.substringAfter("?", "")
                if (query.isNotEmpty()) {
                    query.split("&").forEach { param ->
                        val key = param.substringBefore("=").lowercase()
                        val value = decodeUrlComponent(param.substringAfter("=", ""))
                        when (key) {
                            "pa" -> details["UPI ID / VPA"] = value
                            "pn" -> details["Payee Name"] = value
                            "am" -> details["Amount"] = value
                            "cu" -> details["Currency"] = value
                        }
                    }
                }
                if (!details.containsKey("UPI ID / VPA")) {
                    details["Payload"] = content
                }
            }
            content.startsWith("bitcoin:", ignoreCase = true) -> {
                paymentType = "Bitcoin Payment"
                val address = content.removePrefix("bitcoin:").removePrefix("BITCOIN:").substringBefore("?")
                details["Type"] = paymentType
                details["Wallet Address"] = address
            }
            content.startsWith("ethereum:", ignoreCase = true) -> {
                paymentType = "Ethereum Payment"
                val address = content.removePrefix("ethereum:").removePrefix("ETHEREUM:").substringBefore("?")
                details["Type"] = paymentType
                details["Wallet Address"] = address
            }
            content.startsWith("solana:", ignoreCase = true) -> {
                paymentType = "Solana Payment"
                val address = content.removePrefix("solana:").removePrefix("SOLANA:").substringBefore("?")
                details["Type"] = paymentType
                details["Wallet Address"] = address
            }
            content.contains("paypal.me", ignoreCase = true) -> {
                paymentType = "PayPal Transfer"
                val username = content.substringAfter("paypal.me/").substringBefore("?")
                details["Type"] = paymentType
                details["PayPal Handle"] = username
                details["Link"] = content
            }
            else -> {
                paymentType = "Payment Link"
                details["Type"] = paymentType
                details["Address/Payload"] = content
            }
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.PAYMENT,
            title = paymentType,
            displayDetails = details,
            format = format,
            isSecure = true
        )
    }

    private fun decodeUrlComponent(value: String): String {
        return try {
            java.net.URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }

    private fun parseWhatsapp(url: String, format: String): QrCodeData {
        val safeUrl = if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            "https://$url"
        } else url

        var phone = ""
        var message = ""

        try {
            val query = safeUrl.substringAfter("?", "")
            val path = safeUrl.substringBefore("?")

            if (path.contains("wa.me/", ignoreCase = true)) {
                val rawPhone = path.substringAfter("wa.me/").substringBefore("/")
                phone = rawPhone.filter { it.isDigit() || it == '+' }
            } else if (query.isNotEmpty()) {
                query.split("&").forEach { param ->
                    val key = param.substringBefore("=").lowercase()
                    val value = decodeUrlComponent(param.substringAfter("=", ""))
                    if (key == "phone") phone = value.filter { it.isDigit() || it == '+' }
                }
            }

            if (query.isNotEmpty()) {
                query.split("&").forEach { param ->
                    val key = param.substringBefore("=").lowercase()
                    val value = decodeUrlComponent(param.substringAfter("=", ""))
                    if (key == "text") message = value
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        val details = mutableMapOf<String, String>()
        val formattedPhone = if (phone.startsWith("+")) phone else if (phone.isNotEmpty()) "+$phone" else ""
        if (formattedPhone.isNotEmpty()) details["Phone Number"] = formattedPhone
        if (message.isNotEmpty()) details["Predefined Message"] = message
        details["Link"] = safeUrl

        return QrCodeData(
            rawContent = safeUrl,
            type = QrType.WHATSAPP,
            title = if (formattedPhone.isNotEmpty()) "WhatsApp: $formattedPhone" else "WhatsApp Chat",
            displayDetails = details,
            format = format,
            isSecure = true
        )
    }

    private fun isSocialUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("instagram.com") ||
                lower.contains("twitter.com") ||
                lower.contains("x.com") ||
                lower.contains("tiktok.com") ||
                lower.contains("linkedin.com") ||
                lower.contains("youtube.com") ||
                lower.contains("github.com") ||
                lower.contains("facebook.com")
    }
}
