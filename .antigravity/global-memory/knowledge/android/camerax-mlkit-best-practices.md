# CameraX & ML Kit Barcode Scanning Best Practices

## 1. Rotasi & Analisis Frame
Saat menganalisis gambar langsung dari kamera:
```kotlin
val mediaImage = imageProxy.image
if (mediaImage != null) {
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnCompleteListener {
            // WAJIB: Selalu panggil close() agar buffer kamera tidak deadlock / freeze
            imageProxy.close()
        }
}
```

## 2. Dynamic Format Extraction
Jangan men-hardcode format barcode sebagai `QR_CODE` saat memindai dari sumber gambar/galeri.
Dapatkan format dari `barcode.format`:
- `Barcode.FORMAT_EAN_13` -> `"EAN_13"`
- `Barcode.FORMAT_CODE_128` -> `"CODE_128"`
- `Barcode.FORMAT_UPC_A` -> `"UPC_A"`
- `Barcode.FORMAT_QR_CODE` -> `"QR_CODE"`

## 3. Dynamic Model Delivery
Tambahkan metadata di `AndroidManifest.xml` agar model ML Kit diunduh otomatis oleh Google Play Services tanpa menambah beban ukuran APK:
```xml
<meta-data
    android:name="com.google.mlkit.vision.DEPENDENCIES"
    android:value="barcode" />
```
