package com.scanflow.qr.database

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.database.DateConverters
import org.junit.Test
import java.util.Date

class DatabaseConvertersTest {

    private val converters = DateConverters()

    @Test
    fun `fromTimestamp converts valid epoch millis to Date`() {
        val millis = 1717200000000L
        val date = converters.fromTimestamp(millis)

        assertThat(date).isNotNull()
        assertThat(date?.time).isEqualTo(millis)
    }

    @Test
    fun `fromTimestamp returns null for null input`() {
        val date = converters.fromTimestamp(null)
        assertThat(date).isNull()
    }

    @Test
    fun `dateToTimestamp converts Date to epoch millis`() {
        val millis = 1717200000000L
        val date = Date(millis)
        val result = converters.dateToTimestamp(date)

        assertThat(result).isEqualTo(millis)
    }

    @Test
    fun `dateToTimestamp returns null for null input`() {
        val result = converters.dateToTimestamp(null)
        assertThat(result).isNull()
    }

    @Test
    fun `roundtrip conversion preserves exact timestamp`() {
        val originalMillis = System.currentTimeMillis()
        val date = converters.fromTimestamp(originalMillis)
        val convertedMillis = converters.dateToTimestamp(date)

        assertThat(convertedMillis).isEqualTo(originalMillis)
    }
}
