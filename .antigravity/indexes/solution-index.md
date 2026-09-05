# Global Solution Index

Tabel indeks solusi terverifikasi lintas proyek:

| Solusi | Kategori | Masalah yang Diatasi | File Implementasi Baseline | Proyek Referensi |
|---|---|---|---|---|
| **Lookbehind Regex Semicolon Splitter** | Parser / Text | Delimiter berkarakter khusus ter-escape tidak boleh memecah token | `QrCodeParser.kt` (`body.split(Regex("(?<!\\\\);"))`) | ScanFlow QR |
| **Dynamic ML Kit Barcode Format Mapper** | Scanner / Vision | Menjaga format asli barcode (EAN, Code128, Aztec, dll.) dari galeri | `ScannerViewModel.kt` (`getBarcodeFormatName`) | ScanFlow QR |
| **Android 11–14 Package Visibility `<queries>`** | Android / Security | Memastikan `startActivity` Intent dialer, mail, map, dan calendar selalu dikenali | `AndroidManifest.xml` (`<queries>`) | ScanFlow QR |
| **Native Calendar Event Dispatcher** | Intent / OS | Membuka kalender sistem bawaan untuk menyimpan acara vEvent | `IntentHelper.kt` (`CalendarContract.Events.CONTENT_URI`) | ScanFlow QR |
| **Universal Payment / Crypto Intent Launcher** | Intent / FinTech | Meluncurkan aplikasi UPI / Dompet Kripto dengan fallback copy clipboard aman | `IntentHelper.kt` (`openPayment`) | ScanFlow QR |
| **RFC URL Component Encoder / Decoder** | Networking / URI | URL mailto & sms aman dari spasi, tanda pagar, atau ampersand liar | `QrCodeParser.kt` & `CreateQrViewModel.kt` | ScanFlow QR |
| **R8 & ProGuard Max Shrinking** | Build / DevOps | Memangkas ukuran file APK hingga >60% (27.7MB → 8.59MB) | `app/build.gradle.kts` & `proguard-rules.pro` | ScanFlow QR |
