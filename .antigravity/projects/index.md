# Antigravity Project Registry

Daftar seluruh proyek yang terdaftar dalam sistem memori:

---

## 1. ScanFlow QR
- **Repository**: `biputsmk89-a11y/scanflow-qr`
- **Platform**: Android Native (Jetpack Compose, Kotlin, Material 3)
- **Status**: Production Ready (v1.1.0 / Build 10)
- **Teknologi Utama**: CameraX, Google ML Kit, Room SQLite, DataStore, Dagger Hilt, ZXing
- **Major Lessons**:
  - Delimiter Wi-Fi QR harus diparsing dengan regex negative lookbehind agar karakter `\;` tidak memecah token.
  - Pemuatan Intent di Android 11–14 membutuhkan deklarasi `<queries>`.
  - Format barcode galeri harus dipetakan dinamis dari `barcode.format`.
  - Minifikasi R8 dan Resource Shrinking memangkas ukuran APK dari 27.7MB menjadi 8.59MB.
- **Tautan Memori**: [scanflow/](file:///d:/SMK%20Projek/ScanFlow%20QR/.antigravity/projects/scanflow/project-context.md)
