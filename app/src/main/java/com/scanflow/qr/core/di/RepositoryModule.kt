package com.scanflow.qr.core.di

import android.content.Context
import com.scanflow.qr.core.database.ScanFlowDatabase
import com.scanflow.qr.core.datastore.PreferencesManager
import com.scanflow.qr.data.repository.CloudAuthRepositoryImpl
import com.scanflow.qr.data.repository.CloudSyncRepositoryImpl
import com.scanflow.qr.data.repository.FavoriteRepositoryImpl
import com.scanflow.qr.data.repository.HistoryRepositoryImpl
import com.scanflow.qr.data.repository.QrGeneratorRepositoryImpl
import com.scanflow.qr.data.repository.ScanRepositoryImpl
import com.scanflow.qr.data.repository.SettingsRepositoryImpl
import com.scanflow.qr.domain.repository.AuthRepository
import com.scanflow.qr.domain.repository.FavoriteRepository
import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.ScanRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import com.scanflow.qr.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideScanRepository(database: ScanFlowDatabase): ScanRepository {
        return ScanRepositoryImpl(database)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(database: ScanFlowDatabase): HistoryRepository {
        return HistoryRepositoryImpl(database)
    }

    @Provides
    @Singleton
    fun provideQrGeneratorRepository(
        @ApplicationContext context: Context,
        database: ScanFlowDatabase
    ): QrGeneratorRepository {
        return QrGeneratorRepositoryImpl(context, database)
    }

    @Provides
    @Singleton
    fun provideFavoriteRepository(database: ScanFlowDatabase): FavoriteRepository {
        return FavoriteRepositoryImpl(database)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(preferencesManager: PreferencesManager): SettingsRepository {
        return SettingsRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(preferencesManager: PreferencesManager): AuthRepository {
        return CloudAuthRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        @ApplicationContext context: Context,
        database: ScanFlowDatabase,
        preferencesManager: PreferencesManager
    ): SyncRepository {
        return CloudSyncRepositoryImpl(context, database, preferencesManager)
    }
}
