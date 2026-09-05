# ScanFlow QR — Project Context

## Ringkasan Proyek
Aplikasi Android native pemindai dan generator kode QR & barcode produksi tinggi, dibangun dengan Jetpack Compose, Material 3, Clean Architecture (MVVM), dan Google Stitch Design System.

## Metadata Proyek
- **Package Name / Application ID**: `com.scanflow.qr`
- **Current Version**: `1.1.0` (versionCode `10`)
- **Target SDK**: `34` | **Compile SDK**: `34` | **Min SDK**: `24`
- **Bahasa**: Kotlin 1.9.23
- **Build System**: Gradle 8.4 (KTS) + KSP + Dagger Hilt
- **UI Toolkit**: Jetpack Compose BOM 2024.04.00 + Material 3

## Fitur Utama
1. **Scanner Engine**: CameraX & ML Kit Multi-Format (12 format) + Batch Mode + Gallery Image Picker.
2. **QR Parser**: Deteksi 11 kategori format dengan Heuristic URL Security Checker.
3. **QR Studio**: Generator 11 jenis QR dengan kustomisasi warna, dots pattern, eye style, serta ekspor PNG, SVG, dan PDF.
4. **Offline Persistence**: Room SQLite Database dan DataStore Preferences.
5. **Security & Biometrics**: AndroidX BiometricPrompt (Fingerprint/Face) + PIN fallback.

## Keberadaan Berkas Rilis
- **Production AAB**: `release-builds/ScanFlow-QR-v1.1.0-release.aab` (14.69 MB)
- **Production APK**: `release-builds/ScanFlow-QR-v1.1.0-release.apk` (8.59 MB)
- **Debug APK**: `release-builds/ScanFlow-QR-v1.1.0-debug.apk` (22.04 MB)
