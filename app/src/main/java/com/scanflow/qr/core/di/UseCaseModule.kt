package com.scanflow.qr.core.di

import com.scanflow.qr.domain.repository.HistoryRepository
import com.scanflow.qr.domain.repository.QrGeneratorRepository
import com.scanflow.qr.domain.repository.ScanRepository
import com.scanflow.qr.domain.repository.SettingsRepository
import com.scanflow.qr.domain.usecase.AssessUrlSecurityUseCase
import com.scanflow.qr.domain.usecase.DeleteHistoryUseCase
import com.scanflow.qr.domain.usecase.ExportQrCodeUseCase
import com.scanflow.qr.domain.usecase.GenerateQrCodeUseCase
import com.scanflow.qr.domain.usecase.GetAnalyticsSummaryUseCase
import com.scanflow.qr.domain.usecase.GetHistoryUseCase
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.ParseQrCodeUseCase
import com.scanflow.qr.domain.usecase.SaveScanResultUseCase
import com.scanflow.qr.domain.usecase.ShareQrCodeUseCase
import com.scanflow.qr.domain.usecase.ToggleFavoriteUseCase
import com.scanflow.qr.domain.usecase.UpdateSettingsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideParseQrCodeUseCase(repository: ScanRepository): ParseQrCodeUseCase {
        return ParseQrCodeUseCase(repository)
    }

    @Provides
    fun provideSaveScanResultUseCase(repository: ScanRepository): SaveScanResultUseCase {
        return SaveScanResultUseCase(repository)
    }

    @Provides
    fun provideGenerateQrCodeUseCase(repository: QrGeneratorRepository): GenerateQrCodeUseCase {
        return GenerateQrCodeUseCase(repository)
    }

    @Provides
    fun provideGetHistoryUseCase(repository: HistoryRepository): GetHistoryUseCase {
        return GetHistoryUseCase(repository)
    }

    @Provides
    fun provideToggleFavoriteUseCase(
        historyRepository: HistoryRepository,
        qrRepository: QrGeneratorRepository
    ): ToggleFavoriteUseCase {
        return ToggleFavoriteUseCase(historyRepository, qrRepository)
    }

    @Provides
    fun provideDeleteHistoryUseCase(repository: HistoryRepository): DeleteHistoryUseCase {
        return DeleteHistoryUseCase(repository)
    }

    @Provides
    fun provideExportQrCodeUseCase(repository: QrGeneratorRepository): ExportQrCodeUseCase {
        return ExportQrCodeUseCase(repository)
    }

    @Provides
    fun provideShareQrCodeUseCase(repository: QrGeneratorRepository): ShareQrCodeUseCase {
        return ShareQrCodeUseCase(repository)
    }

    @Provides
    fun provideAssessUrlSecurityUseCase(): AssessUrlSecurityUseCase {
        return AssessUrlSecurityUseCase()
    }

    @Provides
    fun provideGetSettingsUseCase(repository: SettingsRepository): GetSettingsUseCase {
        return GetSettingsUseCase(repository)
    }

    @Provides
    fun provideUpdateSettingsUseCase(repository: SettingsRepository): UpdateSettingsUseCase {
        return UpdateSettingsUseCase(repository)
    }

    @Provides
    fun provideGetAnalyticsSummaryUseCase(
        historyRepository: HistoryRepository,
        qrRepository: QrGeneratorRepository
    ): GetAnalyticsSummaryUseCase {
        return GetAnalyticsSummaryUseCase(historyRepository, qrRepository)
    }
}
