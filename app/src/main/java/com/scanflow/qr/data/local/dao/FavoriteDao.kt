package com.scanflow.qr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scanflow.qr.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE referenceId = :refId AND referenceType = :refType")
    suspend fun deleteFavorite(refId: Long, refType: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE referenceId = :refId AND referenceType = :refType)")
    suspend fun isFavorite(refId: Long, refType: String): Boolean
}
