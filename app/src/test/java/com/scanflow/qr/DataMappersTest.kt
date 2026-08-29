package com.scanflow.qr

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.data.local.entity.QrCodeEntity
import com.scanflow.qr.data.local.entity.ScanHistoryEntity
import com.scanflow.qr.data.mapper.toDomain
import com.scanflow.qr.data.mapper.toEntity
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import com.scanflow.qr.domain.model.UserQrCode
import org.junit.Test

class DataMappersTest {

    @Test
    fun `ScanHistoryEntity maps to domain ScanHistoryItem accurately`() {
        val entity = ScanHistoryEntity(
            id = 42,
            type = "WIFI",
            title = "Office WiFi",
            content = "WIFI:S:Office;P:pass;;",
            format = "QR_CODE",
            createdAt = 1700000000000L,
            isFavorite = true,
            isSecure = true
        )

        val domain = entity.toDomain()

        assertThat(domain.id).isEqualTo(42)
        assertThat(domain.type).isEqualTo(QrType.WIFI)
        assertThat(domain.title).isEqualTo("Office WiFi")
        assertThat(domain.isFavorite).isTrue()

        val remappedEntity = domain.toEntity()
        assertThat(remappedEntity.id).isEqualTo(entity.id)
        assertThat(remappedEntity.type).isEqualTo(entity.type)
    }

    @Test
    fun `QrCodeEntity maps to domain UserQrCode accurately`() {
        val entity = QrCodeEntity(
            id = 10,
            type = "WEBSITE",
            title = "My Portfolio",
            content = "https://example.com",
            foregroundColor = 0xFF0052FF.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt(),
            patternStyle = "DOTS",
            eyeStyle = "ROUNDED",
            logoPath = null,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            isFavorite = true,
            scanCount = 5,
            shareCount = 2,
            downloadCount = 1
        )

        val domain = entity.toDomain()

        assertThat(domain.id).isEqualTo(10)
        assertThat(domain.type).isEqualTo(QrType.WEBSITE)
        assertThat(domain.patternStyle).isEqualTo("DOTS")
        assertThat(domain.eyeStyle).isEqualTo("ROUNDED")
        assertThat(domain.scanCount).isEqualTo(5)

        val remappedEntity = domain.toEntity()
        assertThat(remappedEntity.patternStyle).isEqualTo(entity.patternStyle)
        assertThat(remappedEntity.foregroundColor).isEqualTo(entity.foregroundColor)
    }
}
