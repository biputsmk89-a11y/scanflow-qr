package com.scanflow.qr.domain.model

data class QrStyleConfig(
    val foregroundColor: Int = 0xFF000000.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val patternStyle: QrPatternStyle = QrPatternStyle.SQUARE,
    val cornerEyeStyle: QrCornerStyle = QrCornerStyle.SQUARE,
    val size: Int = 512,
    val margin: Int = 2
)

enum class QrPatternStyle(val title: String) {
    SQUARE("Square"),
    ROUNDED("Rounded"),
    DOTS("Dots"),
    DIAMOND("Diamond")
}

enum class QrCornerStyle(val title: String) {
    SQUARE("Square Eye"),
    ROUNDED("Rounded Eye"),
    CIRCLE("Circle Eye")
}

data class SecurityAssessment(
    val isSecure: Boolean,
    val securityLevel: SecurityLevel,
    val summary: String,
    val details: List<String> = emptyList(),
    val isHttps: Boolean = false,
    val domain: String? = null
)

enum class SecurityLevel {
    SAFE,
    WARNING,
    DANGEROUS,
    UNKNOWN
}
