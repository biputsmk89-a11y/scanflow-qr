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
            return QrCodeData(
                rawContent = trimmed,
                type = QrType.BARCODE,
                title = "Barcode ($format)",
                displayDetails = mapOf("Code" to trimmed, "Symbology" to format),
                format = format,
                isSecure = true
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

        val body = content.substringAfter("WIFI:").substringBefore(";;")
        val parts = body.split(";")
        for (part in parts) {
            when {
                part.startsWith("S:") -> ssid = part.substring(2)
                part.startsWith("P:") -> password = part.substring(2)
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

    private fun parseContact(content: String, format: String): QrCodeData {
        var name = ""
        var phone = ""
        var email = ""
        var org = ""

        val lines = content.lines()
        for (line in lines) {
            val upper = line.uppercase()
            when {
                upper.startsWith("FN:") -> name = line.substring(3).trim()
                upper.startsWith("N:") && name.isEmpty() -> name = line.substring(2).replace(";", " ").trim()
                upper.startsWith("TEL:") || upper.startsWith("TEL;") -> phone = line.substringAfter(":").trim()
                upper.startsWith("EMAIL:") || upper.startsWith("EMAIL;") -> email = line.substringAfter(":").trim()
                upper.startsWith("ORG:") -> org = line.substring(4).trim()
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
                    if (param.startsWith("subject=")) subject = param.substring(8)
                    if (param.startsWith("body=")) body = param.substring(5)
                }
            }
        } else if (content.startsWith("MATMSG:", ignoreCase = true)) {
            val parts = content.substring(7).split(";")
            for (part in parts) {
                if (part.startsWith("TO:")) email = part.substring(3)
                if (part.startsWith("SUB:")) subject = part.substring(4)
                if (part.startsWith("BODY:")) body = part.substring(5)
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
            if (data.contains("body=")) {
                message = data.substringAfter("body=")
            }
        } else if (content.startsWith("smsto:", ignoreCase = true)) {
            val data = content.substring(6)
            phone = data.substringBefore(":")
            message = data.substringAfter(":", "")
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.SMS,
            title = "SMS to $phone",
            displayDetails = buildMap {
                put("Phone Number", phone)
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
        var summary = ""

        val lines = content.lines()
        for (line in lines) {
            when {
                line.startsWith("SUMMARY:", ignoreCase = true) -> {
                    summary = line.substring(8).trim()
                    title = summary
                }
                line.startsWith("LOCATION:", ignoreCase = true) -> location = line.substring(9).trim()
            }
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.CALENDAR,
            title = title,
            displayDetails = buildMap {
                put("Event Title", title)
                if (location.isNotEmpty()) put("Location", location)
            },
            format = format,
            isSecure = true
        )
    }

    private fun parsePayment(content: String, format: String): QrCodeData {
        val paymentType = when {
            content.startsWith("upi://", ignoreCase = true) -> "UPI Payment"
            content.startsWith("bitcoin:", ignoreCase = true) -> "Bitcoin Payment"
            content.startsWith("ethereum:", ignoreCase = true) -> "Ethereum Payment"
            content.startsWith("solana:", ignoreCase = true) -> "Solana Payment"
            content.contains("paypal.me", ignoreCase = true) -> "PayPal Transfer"
            else -> "Payment Link"
        }

        return QrCodeData(
            rawContent = content,
            type = QrType.PAYMENT,
            title = paymentType,
            displayDetails = mapOf("Type" to paymentType, "Address/Payload" to content),
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
