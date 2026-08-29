package com.scanflow.qr.data.mapper

import com.scanflow.qr.data.local.entity.QrCodeEntity
import com.scanflow.qr.data.local.entity.ScanHistoryEntity
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode

fun ScanHistoryEntity.toDomain(): ScanHistoryItem {
    return ScanHistoryItem(
        id = id,
        type = QrType.fromString(type),
        title = title,
        content = content,
        format = format,
        createdAt = createdAt,
        isFavorite = isFavorite,
        isSecure = isSecure,
        safetyWarning = safetyWarning
    )
}

fun ScanHistoryItem.toEntity(): ScanHistoryEntity {
    return ScanHistoryEntity(
        id = id,
        type = type.name,
        title = title,
        content = content,
        format = format,
        createdAt = createdAt,
        isFavorite = isFavorite,
        isSecure = isSecure,
        safetyWarning = safetyWarning
    )
}

fun QrCodeEntity.toDomain(): UserQrCode {
    return UserQrCode(
        id = id,
        type = QrType.fromString(type),
        title = title,
        content = content,
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor,
        patternStyle = patternStyle,
        eyeStyle = eyeStyle,
        logoPath = logoPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorite = isFavorite,
        scanCount = scanCount,
        shareCount = shareCount,
        downloadCount = downloadCount
    )
}

fun UserQrCode.toEntity(): QrCodeEntity {
    return QrCodeEntity(
        id = id,
        type = type.name,
        title = title,
        content = content,
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor,
        patternStyle = patternStyle,
        eyeStyle = eyeStyle,
        logoPath = logoPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorite = isFavorite,
        scanCount = scanCount,
        shareCount = shareCount,
        downloadCount = downloadCount
    )
}
