# ScanFlow QR — Architecture

## Layer Architecture
1. **Presentation Layer (`com.scanflow.qr.feature.*`)**:
   - Berbasis Jetpack Compose sepenuhnya.
   - Menggunakan pattern unidirectional data flow (UDF) via `StateFlow` dan `UiState` data classes.
   - ViewModel diinjeksi via Dagger Hilt (`@HiltViewModel`).

2. **Domain Layer (`com.scanflow.qr.domain.*`)**:
   - Model murni Kotlin (`QrCodeData`, `QrType`, `ScanHistoryItem`, `AppSettings`).
   - UseCases spesifik yang membungkus logika bisnis tunggal (misal: `ParseQrCodeUseCase`, `SaveScanResultUseCase`, `ToggleFavoriteUseCase`).
   - Interface Repository murni tanpa ketergantungan framework Android.

3. **Data Layer (`com.scanflow.qr.data.*`)**:
   - Implementasi Repository (`HistoryRepositoryImpl`, `QrGeneratorRepositoryImpl`, `SettingsRepositoryImpl`).
   - Room Database (`ScanFlowDatabase`), Entities (`ScanHistoryEntity`, `QrCodeEntity`, `FavoriteEntity`), dan DAOs.
   - DataStore Preferences untuk konfigurasi pengguna.

4. **Core Layer (`com.scanflow.qr.core.*`)**:
   - `designsystem/`: Dimens, Color, Shape, Typography, Theme, dan ScanFlow UI widgets.
   - `utils/`: IntentHelper, QrCodeParser, QrCodeGenerator, VibratorHelper, ShareHelper.
   - `security/`: UrlSecurityChecker.
   - `crash/`: Global crash recovery handler.
