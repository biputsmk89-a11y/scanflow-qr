package com.scanflow.qr.generator

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.di.FakeQrGeneratorRepository
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.feature.generator.CreateQrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateQrViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var qrRepository: FakeQrGeneratorRepository
    private lateinit var viewModel: CreateQrViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        qrRepository = FakeQrGeneratorRepository()
        viewModel = CreateQrViewModel(qrRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectType updates selectedType and clears error message`() = runTest(testDispatcher) {
        viewModel.generateAndSaveQr {}
        assertThat(viewModel.uiState.value.errorMessage).isNotNull()

        viewModel.selectType(QrType.WIFI)
        assertThat(viewModel.uiState.value.selectedType).isEqualTo(QrType.WIFI)
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `generateAndSaveQr fails when required fields are empty`() = runTest(testDispatcher) {
        var callbackCalled = false
        viewModel.selectType(QrType.WEBSITE)
        viewModel.updateField { copy(textOrUrl = "") }

        viewModel.generateAndSaveQr { callbackCalled = true }
        advanceUntilIdle()

        assertThat(callbackCalled).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Please fill in the required fields.")
    }

    @Test
    fun `generateAndSaveQr with WEBSITE automatically prefixes https if missing`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.WEBSITE)
        viewModel.updateField { copy(textOrUrl = "scanflow.app/docs") }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.content).isEqualTo("https://scanflow.app/docs")
        assertThat(savedQr.title).isEqualTo("https://scanflow.app/docs")
    }

    @Test
    fun `generateAndSaveQr with WIFI builds standard WIFI payload`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.WIFI)
        viewModel.updateField {
            copy(
                wifiSsid = "SMK_LAB_WIFI",
                wifiPassword = "securePassword123",
                wifiSecurity = "WPA",
                wifiHidden = false
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.WIFI)
        assertThat(savedQr.title).isEqualTo("Wi-Fi: SMK_LAB_WIFI")
        assertThat(savedQr.content).isEqualTo("WIFI:T:WPA;S:SMK_LAB_WIFI;P:securePassword123;H:false;;")
    }

    @Test
    fun `generateAndSaveQr with CONTACT formats vCard 3_0 specification`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.CONTACT)
        viewModel.updateField {
            copy(
                contactName = "Ahmad Siswa",
                contactPhone = "+62811223344",
                contactEmail = "ahmad@smk.sch.id",
                contactOrg = "SMK Negeri 1"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.CONTACT)
        assertThat(savedQr.title).isEqualTo("Ahmad Siswa")
        assertThat(savedQr.content).contains("BEGIN:VCARD")
        assertThat(savedQr.content).contains("FN:Ahmad Siswa")
        assertThat(savedQr.content).contains("TEL:+62811223344")
        assertThat(savedQr.content).contains("EMAIL:ahmad@smk.sch.id")
        assertThat(savedQr.content).contains("END:VCARD")
    }

    @Test
    fun `generateAndSaveQr with EMAIL formats mailto payload with url encoding`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.EMAIL)
        viewModel.updateField {
            copy(
                emailTo = "info@scanflow.io",
                emailSubject = "Feedback",
                emailBody = "Great app!"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.EMAIL)
        assertThat(savedQr.content).isEqualTo("mailto:info@scanflow.io?subject=Feedback&body=Great%20app%21")
    }

    @Test
    fun `generateAndSaveQr with CALENDAR formats full vEvent`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.CALENDAR)
        viewModel.updateField {
            copy(
                calendarTitle = "Tech Workshop",
                calendarLocation = "Hall B",
                calendarDescription = "Hands-on session",
                calendarDate = "20261015"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.CALENDAR)
        assertThat(savedQr.content).contains("SUMMARY:Tech Workshop")
        assertThat(savedQr.content).contains("LOCATION:Hall B")
        assertThat(savedQr.content).contains("DESCRIPTION:Hands-on session")
        assertThat(savedQr.content).contains("DTSTART:20261015")
    }

    @Test
    fun `generateAndSaveQr with PAYMENT UPI formats payee name and amount`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.PAYMENT)
        viewModel.updateField {
            copy(
                paymentType = "UPI",
                paymentAddress = "merchant@bank",
                paymentPayeeName = "Shop Store",
                paymentAmount = "250.00"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.PAYMENT)
        assertThat(savedQr.content).isEqualTo("upi://pay?pa=merchant@bank&pn=Shop%20Store&am=250.00")
    }

    @Test
    fun `generateAndSaveQr with SOCIAL strips leading at symbol`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.SOCIAL)
        viewModel.updateField {
            copy(
                socialPlatform = "Instagram",
                socialUsername = "@scanflow_app"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.SOCIAL)
        assertThat(savedQr.content).isEqualTo("https://instagram.com/scanflow_app")
        assertThat(savedQr.title).isEqualTo("Instagram: @scanflow_app")
    }

    @Test
    fun `generateAndSaveQr with LOCATION rejects invalid latitude`() = runTest(testDispatcher) {
        var callbackCalled = false
        viewModel.selectType(QrType.LOCATION)
        viewModel.updateField {
            copy(
                locationLat = "150.0", // Invalid: latitude must be between -90 and 90
                locationLng = "106.8"
            )
        }

        viewModel.generateAndSaveQr { callbackCalled = true }
        advanceUntilIdle()

        assertThat(callbackCalled).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isNotNull()
    }

    @Test
    fun `generateAndSaveQr with BARCODE CODE_128 saves barcode correctly`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.BARCODE)
        viewModel.updateField {
            copy(
                barcodeFormat = "CODE_128",
                barcodeContent = "SCANFLOW-PRO-123"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.BARCODE)
        assertThat(savedQr.content).isEqualTo("SCANFLOW-PRO-123")
        assertThat(savedQr.patternStyle).isEqualTo("CODE_128")
        assertThat(savedQr.title).isEqualTo("Barcode (Code 128): SCANFLOW-PRO-123")
    }

    @Test
    fun `generateAndSaveQr with BARCODE EAN_13 validates 13 digits`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.BARCODE)
        viewModel.updateField {
            copy(
                barcodeFormat = "EAN_13",
                barcodeContent = "8991234567890"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.BARCODE)
        assertThat(savedQr.content).isEqualTo("8991234567890")
        assertThat(savedQr.patternStyle).isEqualTo("EAN_13")
        assertThat(savedQr.title).isEqualTo("EAN-13: 8991234567890")
    }

    @Test
    fun `generateAndSaveQr with BARCODE EAN_13 rejects non-digit input`() = runTest(testDispatcher) {
        var callbackCalled = false
        viewModel.selectType(QrType.BARCODE)
        viewModel.updateField {
            copy(
                barcodeFormat = "EAN_13",
                barcodeContent = "ABC1234567" // Invalid: not 12-13 digits
            )
        }

        viewModel.generateAndSaveQr { callbackCalled = true }
        advanceUntilIdle()

        assertThat(callbackCalled).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("EAN-13")
    }

    @Test
    fun `generateAndSaveQr with WHATSAPP builds valid wa_me url with country code and auto text`() = runTest(testDispatcher) {
        var generatedId: Long? = null
        viewModel.selectType(QrType.WHATSAPP)
        viewModel.updateField {
            copy(
                whatsappCountryCode = "+62",
                whatsappPhone = "08123456789",
                whatsappMessage = "Halo, saya tertarik dengan produk ScanFlow!"
            )
        }

        viewModel.generateAndSaveQr { id -> generatedId = id }
        advanceUntilIdle()

        assertThat(generatedId).isNotNull()
        val savedQr = qrRepository.getUserQrById(generatedId!!)
        assertThat(savedQr).isNotNull()
        assertThat(savedQr!!.type).isEqualTo(QrType.WHATSAPP)
        assertThat(savedQr.content).startsWith("https://wa.me/628123456789?text=")
        assertThat(savedQr.content).contains("Halo%2C%20saya%20tertarik")
        assertThat(savedQr.title).isEqualTo("WhatsApp: +628123456789")
    }

    @Test
    fun `normalizeWhatsappNumber handles local zero, existing country prefix, and spaces`() {
        // Local number starting with 0
        assertThat(viewModel.normalizeWhatsappNumber("+62", "0812-3456-7890")).isEqualTo("6281234567890")
        // Number without zero or country code
        assertThat(viewModel.normalizeWhatsappNumber("+62", "812 3456 7890")).isEqualTo("6281234567890")
        // Number already containing country code 62
        assertThat(viewModel.normalizeWhatsappNumber("+62", "+62 812-3456-7890")).isEqualTo("6281234567890")
        // US number (+1)
        assertThat(viewModel.normalizeWhatsappNumber("+1", "(555) 123-4567")).isEqualTo("15551234567")
        // Empty phone
        assertThat(viewModel.normalizeWhatsappNumber("+62", "")).isEmpty()
    }
}

