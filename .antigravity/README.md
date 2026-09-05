# Antigravity Global Project Memory System

## Persistent Cross-Project Engineering Memory & Learning Architecture

Sistem ini adalah lapisan manajemen pengetahuan teknis (*Knowledge Management & Engineering Intelligence*) yang dirancang agar AI Pair Programmer dan tim pengembang belajar secara kumulatif dari setiap proyek yang dikerjakan.

---

### Prinsip Utama:
> **SOLVE ONCE → DOCUMENT ONCE → VERIFY ONCE → REUSE MANY TIMES**

Tidak ada investigasi ulang dari nol untuk masalah teknis yang pernah diselesaikan sebelumnya.

---

### 3 Level Memori:

1. **Level 1 — Global Memory (`.antigravity/global-memory/`)**:
   - Pengetahuan, bug patterns, solusi terverifikasi, dan anti-patterns yang berlaku lintas proyek (Android, React, Next.js, Node.js, PostgreSQL, DevOps, Security, dll.).

2. **Level 2 — Project Memory (`.antigravity/projects/`)**:
   - Memori spesifik per proyek (konteks arsitektur, keputusan ADR, daftar bug & fix, pengujian, deployment, dan lessons learned proyek).

3. **Level 3 — Session Memory (`.antigravity/session-memory/`)**:
   - Rekam jejak pekerjaan yang sedang aktif, file yang baru diubah, bug aktif, dan langkah tindak lanjut (*next actions*).

---

### Perintah Cepat Antigravity:

- `/memory-init <project>` : Memuat konteks global & proyek serta memvalidasi kesiapan.
- `/memory-search <query>` : Mencari solusi historis, pola bug, dan arsitektur serupa.
- `/memory-save` : Menyimpan solusi, bug fix, atau keputusan baru ke memori.
- `/project-context <project>` : Menampilkan ringkasan menyeluruh arsitektur & status proyek.
- `/lesson-save` : Menyimpan pelajaran baru (*post-mortem analysis*) dan mempromosikannya ke memori global jika reusable.
