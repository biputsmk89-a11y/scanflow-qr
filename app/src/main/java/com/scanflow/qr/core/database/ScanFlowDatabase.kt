package com.scanflow.qr.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.scanflow.qr.core.common.Constants
import com.scanflow.qr.data.local.dao.FavoriteDao
import com.scanflow.qr.data.local.dao.QrCodeDao
import com.scanflow.qr.data.local.dao.ScanHistoryDao
import com.scanflow.qr.data.local.entity.FavoriteEntity
import com.scanflow.qr.data.local.entity.QrCodeEntity
import com.scanflow.qr.data.local.entity.ScanHistoryEntity
import java.util.Date

class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Database(
    entities = [
        ScanHistoryEntity::class,
        QrCodeEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class ScanFlowDatabase : RoomDatabase() {

    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun qrCodeDao(): QrCodeDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: ScanFlowDatabase? = null

        fun getInstance(context: Context): ScanFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanFlowDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
