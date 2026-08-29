package com.scanflow.qr.domain.repository

import com.scanflow.qr.domain.model.QrCodeData
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun parseScannedContent(rawContent: String, format: String): QrCodeData
    suspend fun saveScanResult(qrCodeData: QrCodeData): Long
}

interface HistoryRepository {
    fun getAllHistory(): Flow<List<ScanHistoryItem>>
    fun getFavoriteHistory(): Flow<List<ScanHistoryItem>>
    fun searchHistory(query: String): Flow<List<ScanHistoryItem>>
    fun getHistoryByType(type: QrType): Flow<List<ScanHistoryItem>>
    fun getRecentHistory(limit: Int): Flow<List<ScanHistoryItem>>
    fun getTotalScanCount(): Flow<Int>
    fun getScansTodayCount(): Flow<Int>
    suspend fun getHistoryItemById(id: Long): ScanHistoryItem?
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun deleteHistoryItem(id: Long)
    suspend fun deleteHistoryItems(ids: List<Long>)
    suspend fun clearAllHistory()
}
