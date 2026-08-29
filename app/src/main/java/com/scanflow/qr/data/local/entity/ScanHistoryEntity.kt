package com.scanflow.qr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val content: String,
    val format: String,
    val createdAt: Long,
    val isFavorite: Boolean = false,
    val isSecure: Boolean = true,
    val safetyWarning: String? = null
)

@Entity(tableName = "user_qr_codes")
data class QrCodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val content: String,
    val foregroundColor: Int,
    val backgroundColor: Int,
    val patternStyle: String,
    val eyeStyle: String,
    val logoPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    val scanCount: Int = 0,
    val shareCount: Int = 0,
    val downloadCount: Int = 0
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val referenceId: Long,
    val referenceType: String, // "SCAN_HISTORY" or "USER_QR"
    val createdAt: Long
)
