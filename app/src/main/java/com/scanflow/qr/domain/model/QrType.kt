package com.scanflow.qr.domain.model

enum class QrType(val displayName: String) {
    WEBSITE("Website / URL"),
    TEXT("Plain Text"),
    WIFI("Wi-Fi Network"),
    CONTACT("Contact (vCard)"),
    EMAIL("Email Address"),
    PHONE("Phone Call"),
    SMS("SMS Message"),
    LOCATION("Geo Location"),
    CALENDAR("Calendar Event"),
    PAYMENT("Payment / Crypto"),
    SOCIAL("Social Media"),
    BARCODE("Barcode"),
    WHATSAPP("WhatsApp Chat");

    companion object {
        fun fromString(typeStr: String): QrType {
            return entries.firstOrNull { it.name.equals(typeStr, ignoreCase = true) } ?: TEXT
        }
    }
}
