# Recent Changes Log

## Berkas yang Baru Dimodifikasi (v1.1.0):
1. `app/build.gradle.kts`: Naik ke versionCode 10, versionName 1.1.0.
2. `app/src/main/AndroidManifest.xml`: Menambahkan tag `<queries>` untuk package visibility Android 11–14.
3. `app/src/main/java/com/scanflow/qr/core/utils/IntentHelper.kt`: Menambahkan `addCalendarEvent` dan `openPayment`.
4. `app/src/main/java/com/scanflow/qr/core/utils/QrCodeParser.kt`: Regex lookbehind split Wi-Fi, full vEvent parser, unescape, URL component decode, dan UPI detail extractor.
5. `app/src/main/java/com/scanflow/qr/feature/generator/CreateQrViewModel.kt`: Field calendar date & description, payment payee & amount, Wi-Fi escaping, RFC url-encode, username sanitasi.
6. `app/src/main/java/com/scanflow/qr/feature/generator/CreateQrScreen.kt`: Switch Hidden Network di UI, field tanggal/waktu & deskripsi kalender, field payee name & amount.
7. `app/src/main/java/com/scanflow/qr/feature/result/ScanResultScreen.kt`: Icon dan label dinamis untuk Kalender & Pembayaran.
8. `app/src/main/java/com/scanflow/qr/feature/result/ScanResultViewModel.kt`: Dispatch aksi native Intent untuk Kalender dan Pembayaran.
9. `app/src/main/java/com/scanflow/qr/feature/scanner/ScannerViewModel.kt`: Mapping format barcode galeri dinamis (`barcode.format`).
10. `app/src/test/java/com/scanflow/qr/QrCodeParserTest.kt`: Tes unescape Wi-Fi, vEvent lengkap, URL encoding, dan UPI details.
11. `app/src/test/java/com/scanflow/qr/generator/CreateQrViewModelTest.kt`: Tes 11 fitur QR lengkap dan validasi koordinat.
12. `docs/FINAL-BUILD-VERIFICATION.md`: Catatan verifikasi build v1.1.0.
13. `release-builds/`: Berkas `ScanFlow-QR-v1.1.0-release.apk`, `ScanFlow-QR-v1.1.0-release.aab`, dan `ScanFlow-QR-v1.1.0-debug.apk`.
