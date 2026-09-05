# Antigravity Development Rules

Pedoman rekayasa perangkat lunak untuk menjaga kualitas tinggi di setiap iterasi:

## 1. Clean Architecture & Separations of Concerns
- UI layer (Jetpack Compose / React / Frontend) hanya bertugas menampilkan state dan menerima input.
- Domain layer (Models, UseCases, Interfaces) murni independen dari framework.
- Data layer (Room, Retrofit, Repositories, DataStore) mengelola persistensi dan sumber data.

## 2. Dynamic Aesthetics & Design Systems
- Gunakan token desain terpusat (`Dimens`, `Color`, `Shape`, `Typography`, `Theme`).
- Hindari hardcoded values di dalam file screen individual.
- Terapkan micro-interactions, dark/light mode harmonis, dan penanganan loading/error state yang elegan.

## 3. Play Store & Production Standards
- Target SDK selalu mengikuti regulasi terbaru Google Play Console (`targetSdk = 34+`).
- Selalu uji build minifikasi R8 (`isMinifyEnabled = true`, `isShrinkResources = true`).
- Deklarasikan `<queries>` secara tepat untuk intent compatibility di Android 11+.

## 4. Verification Protocol
- Setiap perubahan kode harus disertai unit test atau pembaruan test suite.
- Jalankan pemeriksaan lint dan build rilis sebelum commit/push ke repository.
