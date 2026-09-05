# Antigravity Debugging Rules

Pedoman investigasi dan penyelesaian masalah teknis:

## 1. Alur Investigasi Wajib
```text
GEJALA / ERROR LOG
      ↓
KLASIFIKASI KATEGORI
      ↓
CARI MEMORI TERKAIT
      ↓
PERIKSA PENYEBAB HISTORIS & SOLUSI SEBELUMNYA
      ↓
PERIKSA DAFTAR EKSPERIMEN GAGAL (AGAR TIDAK DIULANG)
      ↓
SUSUN HIPOTESIS & VERIFIKASI DENGAN TES
      ↓
TERAPKAN PERBAIKAN TARGETED
      ↓
VERIFIKASI REGRESI (RUN ALL TESTS)
      ↓
DOKUMENTASIKAN LESSON LEARNED KE MEMORI
```

## 2. Larangan Percobaan Buta (No Blind Trial-and-Error)
- Jangan mengubah baris kode secara acak tanpa memahami akar penyebab (*root cause*).
- Jangan menerapkan kembali eksperimen yang sudah tercatat `FAILED` di memori historis.

## 3. Dokumentasi Pasca-Perbaikan (Post-Mortem)
Setiap bug serius wajib menghasilkan:
1. Penjelasan mengapa masalah terjadi (*Why it happened*).
2. Apa yang awalnya dikira salah (*Initial assumption*).
3. Apa akar masalah sebenarnya (*Actual root cause*).
4. Solusi yang sukses (*Verified fix*).
5. Cara mendeteksi dan mencegah di masa depan (*Early detection & prevention*).
