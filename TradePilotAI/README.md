# TradePilot AI — Monorepo

Browser trading pintar (bukan broker) untuk Exness dengan AI Copilot
(analisa chart ICT/SMC lewat Gemini), dijalankan lewat Cloudflare Worker
AI Gateway. AI hanya memberi rekomendasi — semua transaksi BUY/SELL tetap
dilakukan manual oleh user.

## Struktur Repo

```
tradepilot-ai/
├── TradePilotAI/        # App Android (Kotlin, Jetpack Compose, multi-module)
│   └── docs/             # Blueprint arsitektur, AUDIT, TESTING, PERFORMANCE
├── cloudflare-worker/    # Backend: AI Gateway (Gemini proxy) + D1 + R2
└── docs/
    └── DEPLOY.md         # Panduan deploy lengkap: Cloudflare -> GitHub -> CI/CD
```

## Mulai Cepat

1. **Deploy backend dulu** — ikuti `docs/DEPLOY.md` Bagian A (Cloudflare Worker + D1 + R2).
2. **Push ke GitHub** — ikuti `docs/DEPLOY.md` Bagian B.
3. **Aktifkan CI/CD** — ikuti `docs/DEPLOY.md` Bagian C (GitHub Actions otomatis build APK & deploy Worker setiap push).
4. **Build & jalankan app** — buka folder `TradePilotAI/` di Android Studio, isi URL Worker + Gateway Token di Settings (lihat `docs/DEPLOY.md` Bagian D).

## Dokumentasi Lengkap

| Dokumen | Isi |
|---|---|
| `TradePilotAI/docs/Blueprint_v0.md` | Arsitektur lengkap: module diagram, database schema, roadmap |
| `TradePilotAI/docs/AUDIT.md` | Hasil audit kode (bug yang ditemukan & diperbaiki) |
| `TradePilotAI/docs/TESTING.md` | Semua pengujian yang dijalankan (Worker: 14+15 test PASS; domain: 32 test PASS) + checklist manual QA |
| `TradePilotAI/docs/PERFORMANCE.md` | Optimasi performa yang diterapkan |
| `TradePilotAI/docs/RELEASE_CHECKLIST.md` | Checklist sebelum rilis beta publik |
| `docs/DEPLOY.md` | Panduan deploy Cloudflare + GitHub + CI/CD, langkah demi langkah |

## Status Saat Ini

Fase 0-9 (fitur inti + arsitektur) selesai, Worker AI Gateway + D1 + R2
terintegrasi & teruji lokal. **Belum pernah di-build dengan Android SDK
sungguhan** dan **belum di-deploy ke Cloudflare sungguhan** — kedua hal ini
perlu kamu jalankan sendiri mengikuti `docs/DEPLOY.md` karena lingkungan
pengembangan ini tidak memiliki akses ke Android SDK maupun kredensial
akun Cloudflare/GitHub kamu.
