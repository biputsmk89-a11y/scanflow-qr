package com.scanflow.qr.core.di

import android.content.Context
import com.scanflow.qr.core.database.ScanFlowDatabase
import com.scanflow.qr.core.datastore.PreferencesManager
import com.scanflow.qr.data.local.dao.FavoriteDao
import com.scanflow.qr.data.local.dao.QrCodeDao
import com.scanflow.qr.data.local.dao.ScanHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScanFlowDatabase {
        return ScanFlowDatabase.getInstance(context)
    }

    @Provides
    fun provideScanHistoryDao(database: ScanFlowDatabase): ScanHistoryDao {
        return database.scanHistoryDao()
    }

    @Provides
    fun provideQrCodeDao(database: ScanFlowDatabase): QrCodeDao {
        return database.qrCodeDao()
    }

    @Provides
    fun provideFavoriteDao(database: ScanFlowDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}
