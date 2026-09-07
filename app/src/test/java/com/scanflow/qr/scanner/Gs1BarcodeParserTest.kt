package com.scanflow.qr.scanner

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.utils.Gs1BarcodeParser
import com.scanflow.qr.core.utils.QrCodeParser
import com.scanflow.qr.domain.model.QrType
import org.junit.Test

class Gs1BarcodeParserTest {

    @Test
    fun `parse Indonesian barcode starting with 899 resolves correctly`() {
        // Indomie barcode standard EAN-13: 8998866200226 (check digit 6)
        val code = "8998866200226"
        val info = Gs1BarcodeParser.parse(code, "EAN_13")

        assertThat(info).isNotNull()
        assertThat(info!!.countryOrType).isEqualTo("Indonesia")
        assertThat(info.flagEmoji).isEqualTo("🇮🇩")
        assertThat(info.prefix).isEqualTo("899")
        assertThat(info.isChecksumValid).isTrue()
    }

    @Test
    fun `parse Singapore barcode starting with 888 resolves correctly`() {
        val code = "8881234567890"
        val info = Gs1BarcodeParser.parse(code, "EAN_13")

        assertThat(info).isNotNull()
        assertThat(info!!.countryOrType).isEqualTo("Singapore")
        assertThat(info.flagEmoji).isEqualTo("🇸🇬")
        assertThat(info.prefix).isEqualTo("888")
    }

    @Test
    fun `parse US and Canada barcode starting with 012 resolves correctly`() {
        val code = "012345678905" // UPC-A 12 digits
        val info = Gs1BarcodeParser.parse(code, "UPC_A")

        assertThat(info).isNotNull()
        assertThat(info!!.countryOrType).isEqualTo("United States & Canada")
        assertThat(info.flagEmoji).isEqualTo("🇺🇸 🇨🇦")
        assertThat(info.prefix).isEqualTo("012")
        assertThat(info.isChecksumValid).isTrue()
    }

    @Test
    fun `parse Japanese barcode starting with 490 resolves correctly`() {
        val code = "4901234567894"
        val info = Gs1BarcodeParser.parse(code, "EAN_13")

        assertThat(info).isNotNull()
        assertThat(info!!.countryOrType).isEqualTo("Japan")
        assertThat(info.flagEmoji).isEqualTo("🇯🇵")
    }

    @Test
    fun `corrupted checksum is flagged as invalid`() {
        // Altering the last check digit of 8998866200226 to 5
        val corruptCode = "8998866200225"
        val info = Gs1BarcodeParser.parse(corruptCode, "EAN_13")

        assertThat(info).isNotNull()
        assertThat(info!!.isChecksumValid).isFalse()
    }

    @Test
    fun `QrCodeParser integrates Gs1BarcodeParser into parsed details and title`() {
        val code = "8998866200226"
        val parsed = QrCodeParser.parse(code, "EAN_13")

        assertThat(parsed.type).isEqualTo(QrType.BARCODE)
        assertThat(parsed.title).contains("Indonesia")
        assertThat(parsed.title).contains("🇮🇩")
        assertThat(parsed.displayDetails["Country / Origin"]).isEqualTo("🇮🇩 Indonesia")
        assertThat(parsed.displayDetails["GS1 Prefix"]).isEqualTo("899")
        assertThat(parsed.displayDetails["Checksum Status"]).contains("Valid")
        assertThat(parsed.isSecure).isTrue()
    }
}
