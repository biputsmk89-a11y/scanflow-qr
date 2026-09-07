package com.scanflow.qr.export

import android.graphics.Bitmap
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.utils.QrCodeGenerator
import com.scanflow.qr.domain.model.QrCornerStyle
import com.scanflow.qr.domain.model.QrExportFormat
import com.scanflow.qr.domain.model.QrPatternStyle
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.feature.preview.QrPreviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

import android.net.TestUri

class FakeQrGeneratorRepository : QrGeneratorRepository {
    var exportedFormats = mutableListOf<String>()
    var downloadCount = 0
    var shareCount = 0
    private val testUri = TestUri()

    val dummyQr = UserQrCode(
        id = 42L,
        title = "Absensi Siswa SMK",
        content = "https://scanflow.app/absen/rpl-12",
        type = QrType.WEBSITE,
        foregroundColor = 0xFF000000.toInt(),
        backgroundColor = 0xFFFFFFFF.toInt(),
        patternStyle = "SQUARE",
        eyeStyle = "SQUARE",
        isFavorite = false
    )

    override fun generateQrBitmap(content: String, config: QrStyleConfig): Bitmap? = null

    override suspend fun saveUserQr(userQrCode: UserQrCode): Long = 42L
    override suspend fun updateUserQr(userQrCode: UserQrCode) {}
    override suspend fun updateUserQrStyle(id: Long, config: QrStyleConfig) {}
    override fun getAllUserQrs(): Flow<List<UserQrCode>> = flowOf(listOf(dummyQr))
    override fun getFavoriteUserQrs(): Flow<List<UserQrCode>> = flowOf(emptyList())
    override suspend fun getUserQrById(id: Long): UserQrCode? = if (id == 42L) dummyQr else null
    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {}
    override suspend fun incrementScanCount(id: Long) {}

    override suspend fun incrementShareCount(id: Long) {
        shareCount++
    }

    override suspend fun incrementDownloadCount(id: Long) {
        downloadCount++
    }

    override suspend fun deleteUserQr(id: Long) {}

    override suspend fun exportQrToGallery(bitmap: Bitmap, title: String): Uri? {
        exportedFormats.add("PNG")
        return testUri
    }

    override suspend fun cacheQrForSharing(bitmap: Bitmap, filename: String): Uri? {
        return testUri
    }

    override fun generateQrSvg(content: String, config: QrStyleConfig): String? {
        return QrCodeGenerator.generateQrSvg(content, config)
    }

    override suspend fun exportQrSvg(svgContent: String, title: String): Uri? {
        exportedFormats.add("SVG")
        return testUri
    }

    override suspend fun cacheQrSvgForSharing(svgContent: String, filename: String): Uri? {
        return testUri
    }

    override suspend fun exportQrPdf(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri? {
        exportedFormats.add("PDF")
        return testUri
    }

    override suspend fun cacheQrPdfForSharing(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri? {
        return testUri
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VectorAndPdfExportTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var qrRepository: FakeQrGeneratorRepository
    private lateinit var viewModel: QrPreviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        qrRepository = FakeQrGeneratorRepository()
        viewModel = QrPreviewViewModel(qrRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generateQrSvg with standard config produces valid W3C SVG XML`() {
        val content = "https://smk.sch.id/absensi"
        val config = QrStyleConfig(
            foregroundColor = 0xFF003366.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt(),
            patternStyle = QrPatternStyle.SQUARE,
            cornerEyeStyle = QrCornerStyle.SQUARE
        )

        val svg = QrCodeGenerator.generateQrSvg(content, config)

        assertThat(svg).isNotNull()
        assertThat(svg).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        assertThat(svg).contains("<svg xmlns=\"http://www.w3.org/2000/svg\"")
        assertThat(svg).contains("viewBox=")
        assertThat(svg).contains("<rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\"/>")
        assertThat(svg).contains("<g fill=\"#003366\">")
        assertThat(svg).contains("<rect x=")
        assertThat(svg).endsWith("</svg>\n")
    }

    @Test
    fun `generateQrSvg with dots pattern style generates circle elements`() {
        val content = "WIFI:S:LabSMK;T:WPA;P:Password123;;"
        val config = QrStyleConfig(
            patternStyle = QrPatternStyle.DOTS,
            cornerEyeStyle = QrCornerStyle.CIRCLE
        )

        val svg = QrCodeGenerator.generateQrSvg(content, config)

        assertThat(svg).isNotNull()
        assertThat(svg).contains("<circle cx=")
        assertThat(svg).contains("r=\"0.4\"")
    }

    @Test
    fun `generateQrSvg with diamond pattern style generates polygon elements`() {
        val content = "PRODUK-GUDANG-9912"
        val config = QrStyleConfig(
            patternStyle = QrPatternStyle.DIAMOND,
            cornerEyeStyle = QrCornerStyle.SQUARE
        )

        val svg = QrCodeGenerator.generateQrSvg(content, config)

        assertThat(svg).isNotNull()
        assertThat(svg).contains("<polygon points=")
    }

    @Test
    fun `generateQrSvg with rounded eye style generates rounded rect elements`() {
        val content = "KONTAK-KARTU-NAMA"
        val config = QrStyleConfig(
            cornerEyeStyle = QrCornerStyle.ROUNDED
        )

        val svg = QrCodeGenerator.generateQrSvg(content, config)

        assertThat(svg).isNotNull()
        assertThat(svg).contains("rx=\"0.35\"")
    }

    @Test
    fun `generateQrSvg returns null on empty content`() {
        val svg = QrCodeGenerator.generateQrSvg("")
        assertThat(svg).isNull()
    }

    @Test
    fun `QrExportFormat enum properties are correctly configured`() {
        assertThat(QrExportFormat.PNG.extension).isEqualTo("png")
        assertThat(QrExportFormat.PNG.mimeType).isEqualTo("image/png")

        assertThat(QrExportFormat.SVG.extension).isEqualTo("svg")
        assertThat(QrExportFormat.SVG.mimeType).isEqualTo("image/svg+xml")
        assertThat(QrExportFormat.SVG.displayName).contains("Vektor")

        assertThat(QrExportFormat.PDF.extension).isEqualTo("pdf")
        assertThat(QrExportFormat.PDF.mimeType).isEqualTo("application/pdf")
        assertThat(QrExportFormat.PDF.displayName).contains("Cetak")
    }

    @Test
    fun `viewModel exportQr triggers appropriate repository methods for SVG and PDF`() = runTest {
        viewModel.loadQr(42L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.qrCode).isNotNull()

        // Test SVG export
        viewModel.exportQr(QrExportFormat.SVG)
        advanceUntilIdle()

        assertThat(qrRepository.exportedFormats).contains("SVG")
        assertThat(qrRepository.downloadCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.exportSuccessMessage).contains("SVG Vektor")

        // Test PDF export
        viewModel.exportQr(QrExportFormat.PDF)
        advanceUntilIdle()

        assertThat(qrRepository.exportedFormats).contains("PDF")
        assertThat(qrRepository.downloadCount).isEqualTo(2)
        assertThat(viewModel.uiState.value.exportSuccessMessage).contains("PDF")
    }

    @Test
    fun `generateBarcodeSvg produces valid pure vector SVG for Code 128`() {
        val svg = QrCodeGenerator.generateBarcodeSvg(
            content = "SCANFLOW-12345",
            formatName = "CODE_128",
            foregroundColor = 0xFF000000.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt()
        )
        assertThat(svg).isNotNull()
        assertThat(svg).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        assertThat(svg).contains("<svg xmlns=\"http://www.w3.org/2000/svg\"")
        assertThat(svg).contains("<rect")
        assertThat(svg).contains("</svg>")
    }

    @Test
    fun `generateBarcodeSvg produces valid pure vector SVG for EAN-13`() {
        val svg = QrCodeGenerator.generateBarcodeSvg(
            content = "8991234567890",
            formatName = "EAN_13",
            foregroundColor = 0xFF000000.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt()
        )
        assertThat(svg).isNotNull()
        assertThat(svg).contains("<svg")
        assertThat(svg).contains("viewBox=")
    }

    @Test
    fun `generateBarcodeSvg returns null on empty content`() {
        val svg = QrCodeGenerator.generateBarcodeSvg("")
        assertThat(svg).isNull()
    }
}
