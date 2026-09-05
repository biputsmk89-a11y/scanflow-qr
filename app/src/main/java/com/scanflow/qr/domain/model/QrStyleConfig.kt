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

enum class QrExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val badge: String,
    val description: String
) {
    PNG(
        extension = "png",
        mimeType = "image/png",
        displayName = "PNG (Raster HD)",
        badge = "Gambar",
        description = "Format standar gambar tajam untuk tampilan layar, media sosial, dan chat."
    ),
    SVG(
        extension = "svg",
        mimeType = "image/svg+xml",
        displayName = "SVG (Vektor Industri)",
        badge = "Vektor Murni",
        description = "Resolusi tak terbatas. Tidak akan pernah pecah untuk spanduk, banner, atau desain Figma/Illustrator."
    ),
    PDF(
        extension = "pdf",
        mimeType = "application/pdf",
        displayName = "PDF (Dokumen Siap Cetak)",
        badge = "Format A4",
        description = "Tata letak siap cetak ukuran A4 berbingkai rapi lengkap dengan kop judul dan petunjuk scan."
    )
}
