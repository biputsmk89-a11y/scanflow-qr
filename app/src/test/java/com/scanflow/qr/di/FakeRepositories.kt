package com.scanflow.qr.di

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrStyleConfig
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeHistoryRepository : HistoryRepository {
    private val _history = MutableStateFlow<List<ScanHistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    fun emitItems(items: List<ScanHistoryItem>) {
        _history.value = items
    }

    override fun getAllHistory(): Flow<List<ScanHistoryItem>> = history

    override fun getFavoriteHistory(): Flow<List<ScanHistoryItem>> =
        history.map { list -> list.filter { it.isFavorite } }

    override fun searchHistory(query: String): Flow<List<ScanHistoryItem>> =
        history.map { list -> list.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) } }

    override fun getHistoryByType(type: QrType): Flow<List<ScanHistoryItem>> =
        history.map { list -> list.filter { it.type == type } }

    override fun getRecentHistory(limit: Int): Flow<List<ScanHistoryItem>> =
        history.map { it.take(limit) }

    override fun getTotalScanCount(): Flow<Int> =
        history.map { it.size }

    override fun getScansTodayCount(): Flow<Int> =
        history.map { it.size }

    override suspend fun getHistoryItemById(id: Long): ScanHistoryItem? =
        _history.value.find { it.id == id }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        _history.value = _history.value.map {
            if (it.id == id) it.copy(isFavorite = isFavorite) else it
        }
    }

    override suspend fun deleteHistoryItem(id: Long) {
        _history.value = _history.value.filter { it.id != id }
    }

    override suspend fun deleteHistoryItems(ids: List<Long>) {
        _history.value = _history.value.filter { it.id !in ids }
    }

    override suspend fun clearAllHistory() {
        _history.value = emptyList()
    }
}

class FakeScanRepository : ScanRepository {
    private val savedScans = mutableListOf<QrCodeData>()

    override fun parseScannedContent(rawContent: String, format: String): QrCodeData {
        return QrCodeData(
            rawContent = rawContent,
            format = format,
            type = QrType.TEXT,
            title = "Test QR"
        )
    }

    override suspend fun saveScanResult(qrCodeData: QrCodeData): Long {
        savedScans.add(qrCodeData)
        return savedScans.size.toLong()
    }
}

class FakeQrGeneratorRepository : QrGeneratorRepository {
    private val _createdQrs = MutableStateFlow<List<UserQrCode>>(emptyList())

    override fun generateQrBitmap(content: String, config: QrStyleConfig): Bitmap? = null

    override suspend fun saveUserQr(userQrCode: UserQrCode): Long {
        val newId = (_createdQrs.value.size + 1).toLong()
        _createdQrs.value = _createdQrs.value + userQrCode.copy(id = newId)
        return newId
    }

    override fun getAllUserQrs(): Flow<List<UserQrCode>> = _createdQrs

    override fun getFavoriteUserQrs(): Flow<List<UserQrCode>> =
        _createdQrs.map { list -> list.filter { it.isFavorite } }

    override suspend fun getUserQrById(id: Long): UserQrCode? =
        _createdQrs.value.find { it.id == id }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        _createdQrs.value = _createdQrs.value.map {
            if (it.id == id) it.copy(isFavorite = isFavorite) else it
        }
    }

    override suspend fun incrementScanCount(id: Long) {}
    override suspend fun incrementShareCount(id: Long) {}
    override suspend fun incrementDownloadCount(id: Long) {}

    override suspend fun deleteUserQr(id: Long) {
        _createdQrs.value = _createdQrs.value.filter { it.id != id }
    }

    override suspend fun exportQrToGallery(bitmap: Bitmap, title: String): Uri? = null
    override suspend fun cacheQrForSharing(bitmap: Bitmap, filename: String): Uri? = null
    override fun generateQrSvg(content: String, config: QrStyleConfig): String? = null
    override suspend fun exportQrSvg(svgContent: String, title: String): Uri? = null
    override suspend fun cacheQrSvgForSharing(svgContent: String, filename: String): Uri? = null
    override suspend fun exportQrPdf(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri? = null
    override suspend fun cacheQrPdfForSharing(title: String, type: String, content: String, bitmap: Bitmap?, filename: String): Uri? = null
}
