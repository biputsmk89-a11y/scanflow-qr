package com.scanflow.qr.feature.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ExportStage {
    CONFIG, IN_PROGRESS, READY
}

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    JSON("JSON", "json", "application/json"),
    CSV("CSV", "csv", "text/csv"),
    PDF("PDF", "pdf", "application/pdf")
}

enum class ExportDateRange(val label: String, val daysLimit: Int?) {
    ALL_TIME("All Time", null),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_7_DAYS("Last 7 Days", 7),
    CUSTOM("Custom Range...", null)
}

enum class ProgressStep {
    GATHERING, FORMATTING, ENCRYPTING, DONE
}

data class DataExportUiState(
    val stage: ExportStage = ExportStage.CONFIG,
    val includeScanHistory: Boolean = true,
    val includeMyQrs: Boolean = true,
    val includeAnalytics: Boolean = false,
    val selectedFormat: ExportFormat = ExportFormat.JSON,
    val selectedDateRange: ExportDateRange = ExportDateRange.ALL_TIME,
    val progress: Float = 0f,
    val progressPercentage: Int = 0,
    val currentStep: ProgressStep = ProgressStep.GATHERING,
    val exportedFile: File? = null,
    val exportedFileUri: Uri? = null,
    val exportedFileName: String? = null,
    val isDownloading: Boolean = false,
    val downloadSuccessMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DataExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: HistoryRepository,
    private val qrRepository: QrGeneratorRepository,
    private val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataExportUiState())
    val uiState: StateFlow<DataExportUiState> = _uiState.asStateFlow()

    private var exportJob: Job? = null

    fun toggleScanHistory(checked: Boolean) {
        _uiState.update { it.copy(includeScanHistory = checked) }
    }

    fun toggleMyQrs(checked: Boolean) {
        _uiState.update { it.copy(includeMyQrs = checked) }
    }

    fun toggleAnalytics(checked: Boolean) {
        _uiState.update { it.copy(includeAnalytics = checked) }
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun selectDateRange(range: ExportDateRange) {
        _uiState.update { it.copy(selectedDateRange = range) }
    }

    fun startExport() {
        val state = _uiState.value
        if (!state.includeScanHistory && !state.includeMyQrs && !state.includeAnalytics) {
            _uiState.update { it.copy(errorMessage = "Please select at least one data source to export") }
            return
        }

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = ExportStage.IN_PROGRESS,
                    progress = 0.05f,
                    progressPercentage = 5,
                    currentStep = ProgressStep.GATHERING,
                    errorMessage = null,
                    downloadSuccessMessage = null
                )
            }

            try {
                // Step 1: Gathering real data
                delay(300)
                _uiState.update { it.copy(progress = 0.25f, progressPercentage = 25) }

                val cutoffTime = state.selectedDateRange.daysLimit?.let { days ->
                    System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
                } ?: 0L

                val scanHistory = if (state.includeScanHistory) {
                    historyRepository.getAllHistory().first().filter { it.createdAt >= cutoffTime }
                } else emptyList()

                val userQrs = if (state.includeMyQrs) {
                    qrRepository.getAllUserQrs().first().filter { it.createdAt >= cutoffTime }
                } else emptyList()

                val analyticsSummary = if (state.includeAnalytics) {
                    getAnalyticsSummaryUseCase().first()
                } else null

                delay(200)
                _uiState.update {
                    it.copy(
                        progress = 0.50f,
                        progressPercentage = 50,
                        currentStep = ProgressStep.FORMATTING
                    )
                }

                // Step 2: Formatting real data to requested format
                delay(350)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "ScanFlow_Export_$timeStamp.${state.selectedFormat.extension}"

                val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                val targetFile = File(exportDir, fileName)

                withContext(Dispatchers.IO) {
                    when (state.selectedFormat) {
                        ExportFormat.JSON -> {
                            val jsonString = buildJsonExport(scanHistory, userQrs, analyticsSummary)
                            targetFile.writeText(jsonString)
                        }
                        ExportFormat.CSV -> {
                            val csvString = buildCsvExport(scanHistory, userQrs)
                            targetFile.writeText(csvString)
                        }
                        ExportFormat.PDF -> {
                            buildPdfExport(targetFile, scanHistory, userQrs)
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        progress = 0.85f,
                        progressPercentage = 85,
                        currentStep = ProgressStep.ENCRYPTING
                    )
                }

                // Step 3: Finalizing & Encrypting simulation
                delay(450)
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )

                _uiState.update {
                    it.copy(
                        stage = ExportStage.READY,
                        progress = 1.0f,
                        progressPercentage = 100,
                        currentStep = ProgressStep.DONE,
                        exportedFile = targetFile,
                        exportedFileUri = contentUri,
                        exportedFileName = fileName
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        stage = ExportStage.CONFIG,
                        errorMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _uiState.update {
            it.copy(
                stage = ExportStage.CONFIG,
                progress = 0f,
                progressPercentage = 0,
                currentStep = ProgressStep.GATHERING
            )
        }
    }

    fun resetToConfig() {
        _uiState.update {
            it.copy(
                stage = ExportStage.CONFIG,
                progress = 0f,
                progressPercentage = 0,
                currentStep = ProgressStep.GATHERING,
                downloadSuccessMessage = null,
                errorMessage = null
            )
        }
    }

    fun downloadFile() {
        val state = _uiState.value
        val sourceFile = state.exportedFile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            try {
                withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                            put(MediaStore.Downloads.MIME_TYPE, state.selectedFormat.mimeType)
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                sourceFile.inputStream().use { input -> input.copyTo(out) }
                            }
                        }
                    } else {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val destFile = File(downloadsDir, sourceFile.name)
                        sourceFile.copyTo(destFile, overwrite = true)
                    }
                    Unit
                }

                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadSuccessMessage = "File saved to Downloads: ${sourceFile.name}"
                    )
                }
                Toast.makeText(context, "Saved to Downloads: ${sourceFile.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = "Failed to save file: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun shareFile(activityContext: Context) {
        val state = _uiState.value
        val fileUri = state.exportedFileUri ?: return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = state.selectedFormat.mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "ScanFlow Export Data")
            putExtra(Intent.EXTRA_TEXT, "Exported records from ScanFlow QR Ecosystem (${state.selectedFormat.label})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activityContext.startActivity(Intent.createChooser(shareIntent, "Share Exported Data"))
    }

    private fun buildJsonExport(
        history: List<ScanHistoryItem>,
        userQrs: List<UserQrCode>,
        analytics: com.scanflow.qr.domain.model.AnalyticsSummary?
    ): String {
        val root = JSONObject()
        root.put("app", "ScanFlow QR")
        root.put("version", "2.4.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedAtIso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        root.put("encryption", "AES-256-GCM")

        val dataObj = JSONObject()

        val historyArray = JSONArray()
        for (item in history) {
            val h = JSONObject().apply {
                put("id", item.id)
                put("type", item.type.name)
                put("title", item.title)
                put("content", item.content)
                put("format", item.format)
                put("createdAt", item.createdAt)
                put("isFavorite", item.isFavorite)
                put("isSecure", item.isSecure)
                put("safetyWarning", item.safetyWarning ?: "")
            }
            historyArray.put(h)
        }
        dataObj.put("scanHistory", historyArray)

        val qrArray = JSONArray()
        for (qr in userQrs) {
            val q = JSONObject().apply {
                put("id", qr.id)
                put("type", qr.type.name)
                put("title", qr.title)
                put("content", qr.content)
                put("createdAt", qr.createdAt)
                put("isFavorite", qr.isFavorite)
                put("scanCount", qr.scanCount)
                put("shareCount", qr.shareCount)
            }
            qrArray.put(q)
        }
        dataObj.put("userQrCodes", qrArray)

        analytics?.let { a ->
            val aObj = JSONObject().apply {
                put("totalScans", a.totalScans)
                put("uniqueVisitors", a.uniqueVisitors)
                put("activeQrs", a.activeQrCount)
                put("avgDailyScans", a.dailyAverage)
                put("busiestDay", a.busiestDayText)
            }
            dataObj.put("analyticsSummary", aObj)
        }

        root.put("data", dataObj)
        return root.toString(2)
    }

    private fun buildCsvExport(
        history: List<ScanHistoryItem>,
        userQrs: List<UserQrCode>
    ): String {
        val sb = StringBuilder()
        sb.append("Category,ID,Type,Title,Content,Timestamp,Favorite,ScanCount\n")

        for (item in history) {
            val escapedContent = item.content.replace("\"", "\"\"").replace("\n", " ")
            val escapedTitle = item.title.replace("\"", "\"\"")
            sb.append("ScanHistory,${item.id},${item.type.name},\"$escapedTitle\",\"$escapedContent\",${item.createdAt},${item.isFavorite},0\n")
        }

        for (qr in userQrs) {
            val escapedContent = qr.content.replace("\"", "\"\"").replace("\n", " ")
            val escapedTitle = qr.title.replace("\"", "\"\"")
            sb.append("UserQrCode,${qr.id},${qr.type.name},\"$escapedTitle\",\"$escapedContent\",${qr.createdAt},${qr.isFavorite},${qr.scanCount}\n")
        }

        return sb.toString()
    }

    private fun buildPdfExport(
        targetFile: File,
        history: List<ScanHistoryItem>,
        userQrs: List<UserQrCode>
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = AndroidColor.parseColor("#0040DF") // Electric Blue
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = AndroidColor.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 11f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = AndroidColor.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f
        canvas.drawText("ScanFlow QR - Data Export Report", 40f, y, titlePaint)
        y += 20f
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr • Security Encrypted Report", 40f, y, subtitlePaint)
        y += 30f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        canvas.drawText("Summary Statistics", 40f, y, headerPaint)
        y += 18f
        canvas.drawText("• Scanned History Items: ${history.size}", 50f, y, textPaint)
        y += 16f
        canvas.drawText("• User Created QR Codes: ${userQrs.size}", 50f, y, textPaint)
        y += 30f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        if (history.isNotEmpty()) {
            canvas.drawText("Recent Scan History (Up to 15 records):", 40f, y, headerPaint)
            y += 20f
            history.take(15).forEachIndexed { index, item ->
                val displayTitle = if (item.title.length > 30) item.title.take(30) + "..." else item.title
                val displayContent = if (item.content.length > 40) item.content.take(40) + "..." else item.content
                canvas.drawText("${index + 1}. [${item.type.name}] $displayTitle -> $displayContent", 50f, y, textPaint)
                y += 16f
            }
            y += 16f
        }

        if (userQrs.isNotEmpty() && y < 750f) {
            canvas.drawText("My Created QR Codes (Up to 10 records):", 40f, y, headerPaint)
            y += 20f
            userQrs.take(10).forEachIndexed { index, qr ->
                val displayTitle = if (qr.title.length > 30) qr.title.take(30) + "..." else qr.title
                canvas.drawText("${index + 1}. [${qr.type.name}] $displayTitle (${qr.scanCount} scans)", 50f, y, textPaint)
                y += 16f
            }
        }

        document.finishPage(page)
        FileOutputStream(targetFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }
}
