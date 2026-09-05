# ScanFlow QR — Lessons Learned

## 1. Parsing Delimiter Karakter Khusus pada Wi-Fi QR
- **Masalah**: String Wi-Fi QR seperti `WIFI:T:WPA;S:Office\;Guest;P:Pass\:123;H:true;` terpotong salah saat di-split dengan `split(";")`.
- **Akar Masalah**: Tanda `;` yang di-escape dengan backslash `\;` tetap dianggap sebagai pemisah token.
- **Solusi**: Gunakan regex lookbehind negatif: `body.split(Regex("(?<!\\\\);"))` sehingga tanda titik koma yang didahului garis miring terbalik tidak dianggap pemisah, lalu jalankan fungsi unescaping.
- **Pola Reusable**: Berlaku untuk semua parser format teks bertanda baca (vCard, MeCard, iCal).

## 2. Visibilitas Paket Android 11+
- **Masalah**: Panggilan `Intent.ACTION_VIEW` atau `ACTION_INSERT` melempar `ActivityNotFoundException` di Android 11 s/d 14 meskipun aplikasi browser, kalender, atau email terpasang.
- **Akar Masalah**: Android OS membatasi package querying demi privasi pengguna jika tidak dideklarasikan di `<queries>`.
- **Solusi**: Tambahkan blok `<queries>` di `AndroidManifest.xml` untuk semua skema URI yang digunakan.

## 3. Deteksi Format Barcode Galeri
- **Masalah**: Barcode belanja (EAN-13, Code-128) yang dipindai dari galeri salah diberi label "QR_CODE".
- **Akar Masalah**: Parameter format di-hardcode `"QR_CODE"` di callback pemrosesan gambar galeri.
- **Solusi**: Dapatkan format riil dari `barcode.format` dan konversikan menggunakan mapping format CameraAnalyzer.
