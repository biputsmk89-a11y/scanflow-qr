package com.scanflow.qr

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.utils.QrCodeParser
import com.scanflow.qr.domain.model.QrType
import org.junit.Test

class QrCodeParserTest {

    @Test
    fun `parse url with https correctly classifies as WEBSITE`() {
        val raw = "https://scanflow.app/download"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.WEBSITE)
        assertThat(result.isSecure).isTrue()
        assertThat(result.rawContent).isEqualTo("https://scanflow.app/download")
    }

    @Test
    fun `parse url with http classifies as WEBSITE and flags security warning`() {
        val raw = "http://insecure-site.org/login"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.WEBSITE)
        assertThat(result.isSecure).isFalse()
        assertThat(result.securityWarning).isNotNull()
    }

    @Test
    fun `parse wifi payload correctly parses ssid and password`() {
        val raw = "WIFI:T:WPA;S:MyOfficeWiFi;P:SecretPassword123;H:false;;"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.WIFI)
        assertThat(result.displayDetails["Network (SSID)"]).isEqualTo("MyOfficeWiFi")
        assertThat(result.displayDetails["Password"]).isEqualTo("SecretPassword123")
        assertThat(result.displayDetails["Security"]).isEqualTo("WPA")
    }

    @Test
    fun `parse wifi payload unescapes special characters and tolerates single semicolon`() {
        val raw = "WIFI:T:WPA;S:Office\\;Guest;P:Pass\\:123;H:true;"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.WIFI)
        assertThat(result.displayDetails["Network (SSID)"]).isEqualTo("Office;Guest")
        assertThat(result.displayDetails["Password"]).isEqualTo("Pass:123")
        assertThat(result.displayDetails["Hidden Network"]).isEqualTo("Yes")
    }

    @Test
    fun `parse vCard payload correctly parses contact info`() {
        val raw = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Alice Smith
            TEL:+1234567890
            EMAIL:alice@example.com
            ORG:ScanFlow Tech
            END:VCARD
        """.trimIndent()

        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.CONTACT)
        assertThat(result.title).isEqualTo("Alice Smith")
        assertThat(result.displayDetails["Phone"]).isEqualTo("+1234567890")
        assertThat(result.displayDetails["Email"]).isEqualTo("alice@example.com")
        assertThat(result.displayDetails["Organization"]).isEqualTo("ScanFlow Tech")
    }

    @Test
    fun `parse email with mailto schema correctly parses recipient`() {
        val raw = "mailto:support@scanflow.app?subject=BugReport&body=AppDetails"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.EMAIL)
        assertThat(result.displayDetails["Recipient"]).isEqualTo("support@scanflow.app")
        assertThat(result.displayDetails["Subject"]).isEqualTo("BugReport")
        assertThat(result.displayDetails["Message"]).isEqualTo("AppDetails")
    }

    @Test
    fun `parse phone number correctly identifies PHONE type`() {
        val raw = "tel:+628123456789"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.PHONE)
        assertThat(result.displayDetails["Phone Number"]).isEqualTo("+628123456789")
    }

    @Test
    fun `parse geo location coordinates correctly identifies LOCATION type`() {
        val raw = "geo:-6.2088,106.8456"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.LOCATION)
        assertThat(result.displayDetails["Latitude"]).isEqualTo("-6.2088")
        assertThat(result.displayDetails["Longitude"]).isEqualTo("106.8456")
    }

    @Test
    fun `parse barcode symbology correctly preserves format`() {
        val raw = "8992761136015"
        val result = QrCodeParser.parse(raw, format = "EAN_13")

        assertThat(result.type).isEqualTo(QrType.BARCODE)
        assertThat(result.format).isEqualTo("EAN_13")
        assertThat(result.displayDetails["Code"]).isEqualTo("8992761136015")
    }

    @Test
    fun `parse sms schema correctly identifies SMS type`() {
        val raw = "smsto:+628123456789:Hello from ScanFlow"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.SMS)
        assertThat(result.displayDetails["Phone Number"]).isEqualTo("+628123456789")
        assertThat(result.displayDetails["Message"]).isEqualTo("Hello from ScanFlow")
    }

    @Test
    fun `parse calendar vEvent correctly identifies CALENDAR type`() {
        val raw = """
            BEGIN:VEVENT
            SUMMARY:Quarterly Tech Sprint
            LOCATION:Meeting Room Alpha
            DESCRIPTION:Review and roadmap planning
            DTSTART:20261015T090000
            DTEND:20261015T120000
            END:VEVENT
        """.trimIndent()
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.CALENDAR)
        assertThat(result.title).isEqualTo("Quarterly Tech Sprint")
        assertThat(result.displayDetails["Location"]).isEqualTo("Meeting Room Alpha")
        assertThat(result.displayDetails["Description"]).isEqualTo("Review and roadmap planning")
        assertThat(result.displayDetails["Start Time"]).isEqualTo("20261015T090000")
        assertThat(result.displayDetails["End Time"]).isEqualTo("20261015T120000")
    }

    @Test
    fun `parse crypto and upi payment payloads correctly identifies PAYMENT type`() {
        val rawUpi = "upi://pay?pa=merchant@upi&pn=ScanFlow&am=100.00&cu=INR"
        val resultUpi = QrCodeParser.parse(rawUpi)
        assertThat(resultUpi.type).isEqualTo(QrType.PAYMENT)
        assertThat(resultUpi.title).isEqualTo("UPI Payment")
        assertThat(resultUpi.displayDetails["UPI ID / VPA"]).isEqualTo("merchant@upi")
        assertThat(resultUpi.displayDetails["Payee Name"]).isEqualTo("ScanFlow")
        assertThat(resultUpi.displayDetails["Amount"]).isEqualTo("100.00")
        assertThat(resultUpi.displayDetails["Currency"]).isEqualTo("INR")

        val rawBtc = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val resultBtc = QrCodeParser.parse(rawBtc)
        assertThat(resultBtc.type).isEqualTo(QrType.PAYMENT)
        assertThat(resultBtc.title).isEqualTo("Bitcoin Payment")
        assertThat(resultBtc.displayDetails["Wallet Address"]).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
    }

    @Test
    fun `parse social media profile link correctly identifies SOCIAL type`() {
        val raw = "https://instagram.com/scanflow.official"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.SOCIAL)
        assertThat(result.displayDetails["URL"]).isEqualTo("https://instagram.com/scanflow.official")
    }

    @Test
    fun `parse plain text fallback`() {
        val raw = "Simple plain text memo without schema"
        val result = QrCodeParser.parse(raw)

        assertThat(result.type).isEqualTo(QrType.TEXT)
        assertThat(result.rawContent).isEqualTo(raw)
    }
}
