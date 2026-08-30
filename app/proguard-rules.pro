# ProGuard / R8 Rules for ScanFlow QR Production Release

# 1. Keep Application, Activities, and Architecture Core
-keep class com.scanflow.qr.ScanFlowApplication { *; }
-keep class com.scanflow.qr.MainActivity { *; }
-keep class com.scanflow.qr.core.** { *; }
-keep class com.scanflow.qr.data.** { *; }
-keep class com.scanflow.qr.domain.** { *; }
-keep class com.scanflow.qr.feature.** { *; }

# 2. Room Database (Crucial for Room reflection and KSP generated code)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
    *;
}
-keep class * extends androidx.room.RoomDatabase_Impl { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# 3. Domain Models, Entities & Kotlin Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 4. Google ML Kit Barcode Scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.vision.barcode.**

# 5. CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# 6. ZXing QR Engine & Coil
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keep class coil.** { *; }
-dontwarn coil.**

# 7. AndroidX Biometrics & Security
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# 8. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# 9. Jetpack Compose & Material 3
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.icons.** { *; }
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

