package com.scanflow.qr.data.repository

import com.scanflow.qr.core.database.ScanFlowDatabase
import com.scanflow.qr.core.utils.QrCodeParser
import com.scanflow.qr.data.local.entity.ScanHistoryEntity
import com.scanflow.qr.data.mapper.toDomain
import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class ScanRepositoryImpl(
    private val database: ScanFlowDatabase
) : ScanRepository {

    override fun parseScannedContent(rawContent: String, format: String): QrCodeData {
        return QrCodeParser.parse(rawContent, format)
    }

    override suspend fun saveScanResult(qrCodeData: QrCodeData): Long {
        val entity = ScanHistoryEntity(
            type = qrCodeData.type.name,
            title = qrCodeData.title,
            content = qrCodeData.rawContent,
            format = qrCodeData.format,
            createdAt = System.currentTimeMillis(),
            isFavorite = false,
            isSecure = qrCodeData.isSecure,
            safetyWarning = qrCodeData.securityWarning
        )
        return database.scanHistoryDao().insertScan(entity)
    }
}

class HistoryRepositoryImpl(
    private val database: ScanFlowDatabase
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<ScanHistoryItem>> {
        return database.scanHistoryDao().getAllHistory().map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteHistory(): Flow<List<ScanHistoryItem>> {
        return database.scanHistoryDao().getFavoriteScans().map { list -> list.map { it.toDomain() } }
    }

    override fun searchHistory(query: String): Flow<List<ScanHistoryItem>> {
        return database.scanHistoryDao().searchHistory(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getHistoryByType(type: QrType): Flow<List<ScanHistoryItem>> {
        return database.scanHistoryDao().getHistoryByType(type.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentHistory(limit: Int): Flow<List<ScanHistoryItem>> {
        return database.scanHistoryDao().getRecentHistory(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getTotalScanCount(): Flow<Int> {
        return database.scanHistoryDao().getTotalScanCount()
    }

    override fun getScansTodayCount(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return database.scanHistoryDao().getScansTodayCount(calendar.timeInMillis)
    }

    override suspend fun getHistoryItemById(id: Long): ScanHistoryItem? {
        return database.scanHistoryDao().getHistoryById(id)?.toDomain()
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        database.scanHistoryDao().updateFavoriteStatus(id, isFavorite)
    }

    override suspend fun deleteHistoryItem(id: Long) {
        database.scanHistoryDao().deleteScanById(id)
    }

    override suspend fun deleteHistoryItems(ids: List<Long>) {
        database.scanHistoryDao().deleteScansByIds(ids)
    }

    override suspend fun clearAllHistory() {
        database.scanHistoryDao().clearAllHistory()
    }
}
