# ProGuard / R8 Rules for ScanFlow QR Production Release

# 1. Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep class com.scanflow.qr.data.local.entity.** { *; }
-keep class com.scanflow.qr.data.local.dao.** { *; }

# 2. Domain Models & Serialization
-keep class com.scanflow.qr.domain.model.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# 3. Google ML Kit Barcode Scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.vision.barcode.**

# 4. CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# 5. ZXing QR Engine
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# 6. AndroidX Biometrics
-keep class androidx.biometric.** { *; }

# 7. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# 8. Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
