# ScanFlow QR — Architecture Decisions (ADR)

## ADR-001: Adopsi CameraX & ML Kit Dibandingkan ZXing Embedded untuk Scanner
- **Status**: ACCEPTED
- **Alasan**: CameraX menyediakan lifecycle awareness native Android dan integrasi kamera modern (torch, lens switching) dengan frame rate 30+ FPS. ML Kit barcode engine memiliki kecepatan dan toleransi deteksi kemiringan/cahaya redup yang jauh lebih tinggi daripada ZXing analyzer lama.

## ADR-002: Offline-First Room Architecture
- **Status**: ACCEPTED
- **Alasan**: Menjaga privasi 100% pengguna. Semua kode QR, riwayat, dan data tersimpan lokal di SQLite HP tanpa mengirim satu pun payload ke server luar.

## ADR-003: Zero-Auto-Execute Security Pattern
- **Status**: ACCEPTED
- **Alasan**: Mencegah serangan QR Phishing (Quishing) dan eksekusi payload berbahaya secara otomatis saat memindai kode. Pengguna selalu disajikan ringkasan dan penilaian keamanan terlebih dahulu sebelum melakukan tindakan.

## ADR-004: Versioning v1.1.0 (Build 10) untuk Kepatuhan Play Console
- **Status**: ACCEPTED
- **Alasan**: Memastikan upload rilis ke Google Play Console bebas konflik versi dan memenuhi standar targetSdk 34.
