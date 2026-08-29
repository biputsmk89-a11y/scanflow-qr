package com.scanflow.qr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scanflow.qr.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteScans(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Long): ScanHistoryEntity?

    @Query("SELECT * FROM scan_history WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchHistory(query: String): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE type = :type ORDER BY createdAt DESC")
    fun getHistoryByType(type: String): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<ScanHistoryEntity>>

    @Query("SELECT COUNT(*) FROM scan_history")
    fun getTotalScanCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_history WHERE createdAt >= :startOfDay")
    fun getScansTodayCount(startOfDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity): Long

    @Update
    suspend fun updateScan(scan: ScanHistoryEntity)

    @Query("UPDATE scan_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scan_history WHERE id IN (:ids)")
    suspend fun deleteScansByIds(ids: List<Long>)

    @Query("DELETE FROM scan_history")
    suspend fun clearAllHistory()
}
