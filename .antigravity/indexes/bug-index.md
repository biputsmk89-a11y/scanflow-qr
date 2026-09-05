# Global Bug Index

Tabel indeks referensi cepat pencarian bug di seluruh proyek:

| ID | Bug / Gejala | Teknologi | Akar Masalah | Solusi Terverifikasi | Proyek | Confidence |
|---|---|---|---|---|---|---|
| BUG-001 | Wi-Fi QR dengan tanda baca khusus terpotong salah saat parsing | Android / Kotlin | Delimiter `;` ter-escape (`\;`) terbelah oleh `String.split(";")` | Gunakan regex negative lookbehind `(?<!\\);` lalu unescape `\;` dan `\:` | ScanFlow QR | VERIFIED |
| BUG-002 | Barcode non-QR dari foto galeri terdeteksi sebagai "QR_CODE" | Android / ML Kit | Nilai format barcode di-hardcode `"QR_CODE"` di `scanImageFromGallery` | Petakan format dinamis dari `barcode.format` (EAN-13, Code-128, UPC-A, dll.) | ScanFlow QR | VERIFIED |
| BUG-003 | Intent aksi URL / Telepon / Email / Maps gagal di Android 11+ (API 30–34) | Android / OS | Kebijakan *Package Visibility* membatasi akses antar-aplikasi | Tambahkan deklarasi `<queries>` untuk skema `https`, `tel`, `mailto`, `smsto`, `geo` di `AndroidManifest.xml` | ScanFlow QR | VERIFIED |
| BUG-004 | vEvent Calendar QR tidak memiliki aksi buka kalender | Android / Compose | Intent aksi hanya menyalin raw text tanpa memanggil `CalendarContract.Events` | Panggil `Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)` dengan title, location, description | ScanFlow QR | VERIFIED |
| BUG-005 | Link email & SMS berantakan jika mengandung spasi atau karakter khusus | Android / RFC | Parameter `subject` dan `body` tidak di-encode dengan `URLEncoder.encode` | Terapkan `URLEncoder.encode(..., "UTF-8").replace("+", "%20")` pada pembentukan payload mailto | ScanFlow QR | VERIFIED |
| BUG-006 | Ukuran APK membengkak pada build rilis | Android / Gradle | R8 shrinking atau resource shrinking nonaktif, baseline profiles belum terkompilasi | Aktifkan `isMinifyEnabled = true`, `isShrinkResources = true`, dan optimasi ProGuard rules | ScanFlow QR | VERIFIED |
| BUG-007 | GitHub Actions gagal: `Artifact storage quota has been hit` | GitHub Actions / CI | `actions/upload-artifact@v4` melebihi kuota 500MB dengan retensi 30 hari | Pindahkan `Publish GitHub Release` sebelum artifact upload, beri `continue-on-error: true`, dan turunkan retensi ke 1 hari | ScanFlow QR | VERIFIED |
| BUG-008 | Crash: `Attempt to invoke virtual method 'String BottomNavItem.getRoute()' on null object reference` | Android / Kotlin | Circular static initialization: Companion object mengevaluasi `listOf(Home, ...)` sebelum sub-objects selesai `<clinit>` | Gunakan custom getter `val items: List<BottomNavItem> get() = listOf(...)` dan `.filterNotNull()` | ScanFlow QR | VERIFIED |


