# Android 11–14 Package Visibility `<queries>` Implementation

## Masalah:
Mulai Android 11 (API level 30), sistem Android menerapkan pembatasan visibilitas paket (*Package Visibility*). Aplikasi tidak dapat mendeteksi atau berinteraksi secara mulus dengan aplikasi eksternal (Browser, Dialer, Email Client, Map, Calendar) via `Intent` tanpa deklarasi eksplisit di manifest.

## Solusi di `AndroidManifest.xml`:
```xml
<queries>
    <!-- Web Browser -->
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="https" />
    </intent>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="http" />
    </intent>
    <!-- Dialer -->
    <intent>
        <action android:name="android.intent.action.DIAL" />
        <data android:scheme="tel" />
    </intent>
    <!-- Email Client -->
    <intent>
        <action android:name="android.intent.action.SENDTO" />
        <data android:scheme="mailto" />
    </intent>
    <!-- SMS Client -->
    <intent>
        <action android:name="android.intent.action.SENDTO" />
        <data android:scheme="smsto" />
    </intent>
    <!-- Maps & Geolocation -->
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="geo" />
    </intent>
    <!-- System Calendar Event Insert -->
    <intent>
        <action android:name="android.intent.action.INSERT" />
        <data android:mimeType="vnd.android.cursor.dir/event" />
    </intent>
</queries>
```
Dengan menambahkan deklarasi ini, seluruh pemanggilan Intent pada Android 11 s/d 14 akan selalu berhasil menemukan aplikasi yang sesuai.
