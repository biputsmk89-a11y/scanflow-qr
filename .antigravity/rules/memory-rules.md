# Antigravity Memory Rules

Pedoman wajib pengelolaan dan penggunaan memori lintas proyek:

## 1. Zero Redundancy Protocol
Sebelum menginvestigasi bug baru atau merancang arsitektur baru:
1. Jalankan pencarian di `.antigravity/indexes/bug-index.md` dan `solution-index.md`.
2. Jika ada kecocokan masalah serupa, evaluasi kompatibilitas environment (`MATCH`, `PARTIAL MATCH`, `NO MATCH`).
3. Gunakan solusi terverifikasi sebagai baseline awal, jangan bereksperimen dari nol.

## 2. Tingkat Keyakinan Pengetahuan (Confidence Levels)
- **LOW**: Solusi masih berupa hipotesis, belum diuji tuntas.
- **MEDIUM**: Solusi terbukti berhasil 1 kali pada satu skenario.
- **HIGH**: Solusi berhasil beberapa kali pada skenario berbeda.
- **VERIFIED**: Solusi telah diuji menyeluruh (automated unit tests & production build passing).

## 3. Promosi Pengetahuan ke Global Memory
- Jika solusi bersifat umum dan dapat diterapkan ke proyek lain (misal: R8 ProGuard rules, Android `<queries>` package visibility, URL encoding, JWT authentication, CameraX orientation handling), salin atau rangkum ke `.antigravity/global-memory/`.
- Jika spesifik pada domain bisnis tertentu (misal: aturan kelulusan SPMB), simpan hanya di `.antigravity/projects/<project-name>/`.

## 4. Keamanan Rahasia (Zero Secrets in Memory)
- JANGAN PERNAH menyimpan API key, password, private key, token aktif, atau keystore password di dalam file memori.
- Simpan hanya pola konfigurasi, petunjuk arsitektur, dan referensi nama file properties/environment.

## 5. Versioning & Tidak Menghapus Sejarah
- Jika solusi lama usang atau digantikan oleh standar baru (misal Java 8 Date API digantikan java.time atau library deprecated), tandai status menjadi `SUPERSEDED` dan dokumentasikan penggantinya. Jangan hapus catatan sejarahnya.
