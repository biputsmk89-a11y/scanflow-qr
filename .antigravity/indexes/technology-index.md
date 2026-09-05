# Global Technology Index

Daftar teknologi yang telah dikuasai dan panduan praktisnya:

## 1. Android (Native Kotlin & Jetpack Compose)
- **Compile SDK**: 34 | **Target SDK**: 34 | **Min SDK**: 24.
- **Key Libraries**: CameraX (1.3.2), ML Kit Barcode (17.2.0), Room (2.6.1), Dagger Hilt (2.51.1), DataStore (1.0.0), Compose BOM (2024.04.00).
- **Known Issues**:
  - `startActivity` melempar `ActivityNotFoundException` pada Android 11+ jika `<queries>` tidak dideklarasikan.
  - Camera preview orientasi harus dikonversi dengan rotasi derajat `imageProxy.imageInfo.rotationDegrees`.
  - ML Kit Scanner membutuhkan model barcode unbundled di Play Services (`com.google.mlkit.vision.DEPENDENCIES`).

## 2. R8 / ProGuard Optimization
- **Best Practice**:
  - Pertahankan Serializable fields dan Room DAOs/Entities dari obfuscation.
  - Berikan rule `-dontwarn` untuk dependencies pihak ketiga yang memanggil API opsional.
  - Aktifkan `isShrinkResources = true` bersama `isMinifyEnabled = true` untuk memotong 60%+ ukuran binary.

## 3. ZXing & QR Vector/PDF Rendering
- **Best Practice**:
  - Gunakan `MultiFormatWriter` dengan `BarcodeFormat.QR_CODE` dan `Hints.ERROR_CORRECTION`.
  - Untuk export PDF: gunakan `android.graphics.pdf.PdfDocument` dengan halaman standar A4 (595x842 pt).
  - Untuk export SVG: render path / rect XML vektor langsung tanpa ketergantungan library eksternal berat.
