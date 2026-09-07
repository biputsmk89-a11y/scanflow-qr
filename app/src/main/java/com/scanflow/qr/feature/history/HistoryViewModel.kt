package com.scanflow.qr.feature.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.ScanFlowApplication
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<ScanHistoryItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilterType: QrType? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteHistoryUseCase: DeleteHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilterType = MutableStateFlow<QrType?>(null)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isManualSelectionMode = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> = combine(
        _searchQuery,
        _selectedFilterType,
        _isManualSelectionMode
    ) { query, filterType, manualSelect ->
        Triple(query, filterType, manualSelect)
    }.flatMapLatest { (query, filterType, manualSelect) ->
        val cleanQuery = query.trim()
        val flow = when {
            cleanQuery.isNotEmpty() && filterType != null -> {
                getHistoryUseCase.search(cleanQuery, filterType).catch { emit(emptyList()) }
            }
            cleanQuery.isNotEmpty() -> {
                getHistoryUseCase.search(cleanQuery).catch { emit(emptyList()) }
            }
            filterType != null -> {
                getHistoryUseCase.getByType(filterType).catch { emit(emptyList()) }
            }
            else -> {
                getHistoryUseCase().catch { emit(emptyList()) }
            }
        }
        flow.combine(_selectedIds) { items, selected ->
            HistoryUiState(
                items = items,
                searchQuery = query,
                selectedFilterType = filterType,
                selectedIds = selected,
                isSelectionMode = manualSelect || selected.isNotEmpty(),
                isLoading = false
            )
        }
    }.catch {
        emit(HistoryUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = false)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectFilterType(type: QrType?) {
        _selectedFilterType.value = type
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase.toggleScanFavorite(id, !current)
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
    }

    fun setSelectionMode(enabled: Boolean) {
        _isManualSelectionMode.value = enabled
        if (!enabled) {
            _selectedIds.value = emptySet()
        }
    }

    fun selectAll(allIds: List<Long>) {
        _selectedIds.value = allIds.toSet()
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            deleteHistoryUseCase.deleteItem(id)
            val current = _selectedIds.value.toMutableSet()
            current.remove(id)
            _selectedIds.value = current
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isManualSelectionMode.value = false
    }

    fun deleteSelectedItems() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            deleteHistoryUseCase.deleteItems(ids)
            clearSelection()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            deleteHistoryUseCase.clearAll()
            clearSelection()
        }
    }

    /**
     * Menghasilkan string berformat spreadsheet CSV standar (RFC 4180) dari daftar riwayat pemindaian.
     */
    fun generateCsvString(items: List<ScanHistoryItem>): String {
        val sb = StringBuilder()
        sb.append("ID,Date Time,Timestamp,Format,Category,Title,Content,Favorite,Security Warning\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        for (item in items) {
            val dateStr = dateFormat.format(Date(item.createdAt))
            val escapedTitle = escapeCsvField(item.title)
            val escapedContent = escapeCsvField(item.content)
            val escapedWarning = escapeCsvField(item.safetyWarning ?: "")
            val isFav = if (item.isFavorite) "Yes" else "No"
            val format = item.format.ifEmpty { "QR_CODE" }
            val category = item.type.name

            sb.append("${item.id},\"$dateStr\",${item.createdAt},\"$format\",\"$category\",$escapedTitle,$escapedContent,\"$isFav\",$escapedWarning\n")
        }

        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Membagikan berkas CSV riwayat pindaian melalui Android Share Sheet (WhatsApp, Email, Drive, dll).
     */
    fun shareCsv(context: Context, items: List<ScanHistoryItem>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "Tidak ada data riwayat untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            try {
                val csvContent = generateCsvString(items)
                val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val targetFile = File(exportDir, "ScanFlow_History_$timeStamp.csv")
                targetFile.writeText(csvContent, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )

                // Bypass applock sementara agar tidak terkunci saat dialog share sheet sistem muncul
                (context.applicationContext as? ScanFlowApplication)?.appLockManager?.setTemporarilyBypassed(true)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Ekspor Riwayat ScanFlow QR")
                    putExtra(Intent.EXTRA_TEXT, "Daftar riwayat pemindaian barcode & QR code (${items.size} data) format spreadsheet CSV.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Bagikan Riwayat CSV"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal membagikan CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Menyimpan berkas CSV riwayat pindaian langsung ke URI tujuan (Storage Access Framework).
     */
    fun exportCsvToUri(context: Context, uri: Uri, items: List<ScanHistoryItem>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val csvContent = generateCsvString(items)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}

