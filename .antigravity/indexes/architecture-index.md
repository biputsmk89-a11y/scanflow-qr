# Global Architecture Index

Indeks pola arsitektur terpilih dan status penerapannya:

| Pola Arsitektur | Kategori | Penjelasan & Keunggulan | Proyek Pengguna | Status |
|---|---|---|---|---|
| **Clean Architecture (MVVM)** | Frontend / Mobile | Pemisahan Presentation (Compose), Domain (Pure Kotlin UseCases), dan Data (Room / DataStore) | ScanFlow QR | ACTIVE / RECOMMENDED |
| **Offline-First Room SQLite** | Data Layer | Database lokal sebagai Single Source of Truth (SSoT) tanpa ketergantungan koneksi server | ScanFlow QR | ACTIVE / RECOMMENDED |
| **Zero-Auto-Execute Security Pattern** | Security | Tidak pernah otomatis membuka link atau mengeksekusi payload tanpa persetujuan pengguna | ScanFlow QR | ACTIVE / MANDATORY |
| **Unbundled ML Kit Delivery** | Mobile / AI | Mengunduh model pemindaian barcode dinamis via Google Play Services agar APK tetap kecil | ScanFlow QR | ACTIVE / RECOMMENDED |
| **Centralized Stitch Design System** | UI / UX | Tokenisasi visual terpadu (`Dimens`, `Color`, `Shape`, `Typography`) tanpa duplikasi deklarasi | ScanFlow QR | ACTIVE / RECOMMENDED |
