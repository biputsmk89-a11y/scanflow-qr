package com.scanflow.qr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scanflow.qr.data.local.entity.QrCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrCodeDao {

    @Query("SELECT * FROM user_qr_codes ORDER BY updatedAt DESC")
    fun getAllUserQrs(): Flow<List<QrCodeEntity>>

    @Query("SELECT * FROM user_qr_codes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteUserQrs(): Flow<List<QrCodeEntity>>

    @Query("SELECT * FROM user_qr_codes WHERE id = :id LIMIT 1")
    suspend fun getQrById(id: Long): QrCodeEntity?

    @Query("SELECT COUNT(*) FROM user_qr_codes")
    fun getTotalQrCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQr(qr: QrCodeEntity): Long

    @Update
    suspend fun updateQr(qr: QrCodeEntity)

    @Query("UPDATE user_qr_codes SET foregroundColor = :fg, backgroundColor = :bg, patternStyle = :pattern, eyeStyle = :eye, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQrStyle(id: Long, fg: Int, bg: Int, pattern: String, eye: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_qr_codes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE user_qr_codes SET scanCount = scanCount + 1 WHERE id = :id")
    suspend fun incrementScanCount(id: Long)

    @Query("UPDATE user_qr_codes SET shareCount = shareCount + 1 WHERE id = :id")
    suspend fun incrementShareCount(id: Long)

    @Query("UPDATE user_qr_codes SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: Long)

    @Query("DELETE FROM user_qr_codes WHERE id = :id")
    suspend fun deleteQrById(id: Long)
}
