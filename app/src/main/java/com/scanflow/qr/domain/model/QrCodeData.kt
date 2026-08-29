package com.scanflow.qr.domain.model

data class QrCodeData(
    val rawContent: String,
    val type: QrType,
    val title: String,
    val displayDetails: Map<String, String> = emptyMap(),
    val format: String = "QR_CODE",
    val isSecure: Boolean = true,
    val securityWarning: String? = null
)

data class ScanHistoryItem(
    val id: Long = 0,
    val type: QrType,
    val title: String,
    val content: String,
    val format: String = "QR_CODE",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isSecure: Boolean = true,
    val safetyWarning: String? = null
)

data class UserQrCode(
    val id: Long = 0,
    val type: QrType,
    val title: String,
    val content: String,
    val foregroundColor: Int = 0xFF000000.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val patternStyle: String = "SQUARE",
    val eyeStyle: String = "SQUARE",
    val logoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val scanCount: Int = 0,
    val shareCount: Int = 0,
    val downloadCount: Int = 0
)
