## Mulai Cepat

1. **Deploy Backend Dulu** — ikuti `docs/DEPLOY.md` Bagian A (Cloudflare Workers + D1 + R2 + AI Gateway).
2. **Setup Flutter App** — buka folder `app/`, jalankan `flutter pub get` untuk mengunduh seluruh dependensi.
3. **Konfigurasi Environment** — sesuaikan URL Worker & Access Token di file `.env` atau menu Settings aplikasi.
4. **Jalankan Aplikasi**:
   - **Windows Desktop:** `flutter run -d windows`
   - **Android:** `flutter run -d android`
   - **iOS:** `flutter run -d ios`

## Dokumentasi Lengkap

| Dokumen | Isi |
|---|---|
| `CONSTITUTION.md` | Konstitusi & aturan utama arsitektur proyek TradePilot AI |
| `app/docs/Blueprint_v2.md` | Arsitektur Flutter Feature-First: module diagram, Riverpod state, & UI Workbench |
| `app/docs/AUDIT.md` | Hasil audit kode & performa UI/State Management |
| `app/docs/TESTING.md` | pengujian unit (Riverpod/Dio) & pengujian widget UI |
| `docs/DEPLOY.md` | Panduan deploy Cloudflare + GitHub Actions CI/CD (Multi-Platform Build) |

## Target Platform & Core Features

* **Platform Support:** Windows Desktop, Android, iOS.
* **4 Super-App Hubs:**
  1. **Trading Workspace:** Chart TradingView (WebView Engine), Market Data, & PlutoGrid High-Performance Journal.
  2. **AI Pilot Workspace:** Streaming Chat Copilot & Chart Analysis via Cloudflare AI Gateway (Gemini, Claude, DeepSeek).
  3. **Social & Community:** Trading Chat Room & Signal Sharing.
  4. **Learning & Entertainment:** Educational Hub, Paper Trading, & Lofi Radio Stream.

## Status Saat Ini

* **Backend Stack:** Cloudflare Workers, D1, R2, dan AI Gateway terintegrasi & siap pakai.
* **Frontend:** Refactoring penuh ke **Flutter (Feature-First Architecture)** untuk mendukung performa *Native Cross-Platform*.