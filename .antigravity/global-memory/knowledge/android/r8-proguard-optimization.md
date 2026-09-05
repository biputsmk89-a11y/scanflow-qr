# R8 & ProGuard Optimization Guide

## Konfigurasi Kunci di `build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

## Aturan Wajib ProGuard:
1. **Room Database**:
   ```proguard
   -keep class androidx.room.** { *; }
   -dontwarn androidx.room.**
   -keep class * extends androidx.room.RoomDatabase { public <init>(); *; }
   -keep class * extends androidx.room.RoomDatabase_Impl { *; }
   -keep @androidx.room.Dao interface * { *; }
   -keep @androidx.room.Entity class * { *; }
   ```
2. **Kotlin Serialization & Enums**:
   ```proguard
   -keepclassmembers class * {
       @kotlinx.serialization.Serializable <fields>;
   }
   -keepclassmembers class * extends java.lang.Enum {
       public static **[] values();
       public static ** valueOf(java.lang.String);
   }
   ```
3. **Google ML Kit & CameraX**:
   ```proguard
   -keep class com.google.mlkit.vision.barcode.** { *; }
   -dontwarn com.google.mlkit.vision.barcode.**
   -keep class androidx.camera.** { *; }
   -dontwarn androidx.camera.**
   ```
