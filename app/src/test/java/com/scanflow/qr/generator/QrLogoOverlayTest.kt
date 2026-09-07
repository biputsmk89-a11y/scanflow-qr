package com.scanflow.qr.generator

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.utils.QrCodeGenerator
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.UserQrCode
import org.junit.Test

class QrLogoOverlayTest {

    @Test
    fun `QrStyleConfig holds custom logo properties`() {
        val config = QrStyleConfig(
            size = 512,
            logoPath = "/data/user/0/com.scanflow.qr/files/qr_logos/logo_42.png"
        )

        assertThat(config.logoPath).isEqualTo("/data/user/0/com.scanflow.qr/files/qr_logos/logo_42.png")
        assertThat(config.logoBitmap).isNull()
    }

    @Test
    fun `UserQrCode entity preserves logoPath across styling updates`() {
        val originalQr = UserQrCode(
            id = 42L,
            title = "My Branded QR",
            content = "https://scanflow.app/brand",
            type = QrType.WEBSITE,
            foregroundColor = 0xFF000000.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt(),
            patternStyle = "SQUARE",
            eyeStyle = "SQUARE",
            logoPath = "/files/qr_logos/logo_42.png"
        )

        assertThat(originalQr.logoPath).isEqualTo("/files/qr_logos/logo_42.png")

        val updatedQr = originalQr.copy(
            foregroundColor = 0xFF0066FF.toInt(),
            logoPath = "/files/qr_logos/logo_42_new.png"
        )
        assertThat(updatedQr.logoPath).isEqualTo("/files/qr_logos/logo_42_new.png")
        assertThat(updatedQr.foregroundColor).isEqualTo(0xFF0066FF.toInt())
    }

    @Test
    fun `generateQrSvg without logo generates clean svg markup without image tag`() {
        val configWithoutLogo = QrStyleConfig(size = 512, logoBitmap = null, logoPath = null)

        val svg = QrCodeGenerator.generateQrSvg("https://scanflow.app/clean", configWithoutLogo)
        assertThat(svg).isNotNull()
        assertThat(svg).contains("<svg")
        assertThat(svg).doesNotContain("<image")
        assertThat(svg).contains("</svg>")
    }
}
