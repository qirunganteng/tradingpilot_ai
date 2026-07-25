# TradePilot AI

Browser trading pintar (bukan broker) dengan AI Copilot untuk analisa chart
Exness memakai metode ICT/SMC. AI hanya memberi rekomendasi — semua transaksi
BUY/SELL tetap dilakukan manual oleh user.

## Status: Fase 0 – Fase 9 ✅ (kode ada, belum pernah di-build/run nyata)

| Fase | Fitur | Status |
|---|---|---|
| 0 | Project skeleton multi-module | ✅ |
| 1 | `feature-browser` — WebView Exness + toolbar + tombol ANALISA | ✅ |
| 2 | `feature-screenshot` + `feature-ai` — capture nyata + panggilan Gemini API nyata | ✅ |
| 3 | `feature-drawing` — anotasi legend berwarna di atas screenshot | ✅ (lihat catatan keterbatasan di bawah) |
| 4 | `feature-trading` — Money Management (input manual) | ✅ (lihat catatan keterbatasan) |
| 5 | `feature-journal` — Trading Journal otomatis + statistik (Room) | ✅ |
| 6 | `feature-notification` — AI Copilot polling (hanya saat app dibuka) | ✅ |
| 7 | `feature-analytics` — insight setelah 100 trade | ✅ |
| 8 | `feature-mentor` — evaluasi rule-based per trade | ✅ |
| 9 | Security hardening (root warning, proguard, dsb) | ✅ (parsial, lihat catatan) |
| 10 | Deployment (CI, signing) | Skeleton CI ada, signing config belum |

## ⚠️ PENTING — Belum Pernah Di-build

Kode ini ditulis di lingkungan tanpa Android SDK/Gradle, jadi **belum pernah
benar-benar di-compile**. Sangat mungkin ada typo import, argumen fungsi
Compose API yang berubah antar versi, atau kesalahan kecil lain saat pertama
kali di-sync di Android Studio. Ini normal untuk kode seukuran ini yang
ditulis "buta" — perlakukan sebagai **base implementasi yang solid**, bukan
kode siap produksi. Langkah pertama setelah pindah ke Android Studio:

1. **Sync Gradle** — kemungkinan besar Android Studio otomatis men-generate
   `gradlew`/`gradlew.bat`/`gradle-wrapper.jar` yang belum ada di paket ini
   (dibuat di lingkungan tanpa akses ke `services.gradle.org`). Kalau tidak
   otomatis: `File > Sync Project with Gradle Files`, atau jalankan
   `gradle wrapper --gradle-version 8.9` sekali via Gradle yang sudah
   terinstal di komputer kamu.
2. **Build project** (`Build > Rebuild Project`) dan perbaiki error compile
   satu per satu — kemungkinan besar hanya butuh penyesuaian kecil versi API.
3. Baru jalankan di emulator/device Android 10+.

## Keterbatasan yang Disengaja (Bukan Bug)

1. **Balance/Equity/Margin** di Money Management **diisi manual** — membaca
   DOM Exness butuh JavaScript-bridge yang fragile dan berisiko melanggar
   ToS Exness. Arsitektur (`AccountRepository` interface di domain) sudah
   siap kalau nanti ingin dicoba.
2. **Anotasi chart** (`AnnotationEngine`) menggambar **legend/ringkasan**
   di sisi kanan gambar, BUKAN garis presisi tepat di titik harga — model
   vision umum tidak bisa memberi koordinat piksel akurat untuk Order
   Block/FVG/dll.
3. **AI Copilot** (notifikasi) hanya polling **selama screen Notification
   terbuka** dan user menekan "Mulai Monitor" — sesuai keputusan kamu
   (bukan background service). Interval default 60 detik, bisa diubah di
   `CopilotMonitorViewModel.startMonitoring()`.
4. **Certificate pinning** (Fase 9) baru berupa hook kosong di
   `NetworkModule` — perlu diisi hash sertifikat asli Gemini API/Exness
   sebelum rilis produksi.
5. **Migration Room** masih `fallbackToDestructiveMigration()` — cukup
   untuk development, WAJIB diganti ke migration eksplisit sebelum rilis
   (supaya data user tidak hilang saat update skema).
6. **Root detection** (`RootDetector`) masih heuristik file-check sederhana,
   bukan library dedicated (mis. RootBeer) — cukup untuk skeleton, sebaiknya
   diperkuat sebelum rilis publik.
7. **Bahasa dwibahasa**: string resource ID (default) & EN sudah ada, tapi
   baru dipakai di modul `app` — belum ada UI untuk user memilih bahasa
   manual (masih ikut setting bahasa perangkat).

## Struktur Modul

Lihat `docs/Blueprint_v0.md` bagian 1–3 untuk Project Tree, Module Diagram,
dan Dependency Diagram lengkap. Ringkas: `app` → `feature-*` → `domain` ←
`data-*` → `core-*`. Setiap `feature-*` independen satu sama lain; komunikasi
lintas-fitur (mis. AI Copilot → Notification Center) lewat `EventBus`
(`SharedFlow`) di `core-common`.

## Alur Data Utama (Tombol ANALISA)

```
BrowserScreen (WebView)
  -> tekan ANALISA
  -> AppRootViewModel.onAnalyzeRequested()
  -> ScreenCapture.captureView() + compressToJpeg()
  -> PendingAnalysisHolder.submit()
  -> navigasi ke AnalysisScreen
  -> AnalysisViewModel.consumePendingImageIfAny()
  -> AnalyzeChartUseCase -> AIRepository -> GeminiProvider -> Gemini API
  -> AnalysisResponseMapper -> AnalysisResult
  -> tampil di panel (tidak pernah mengeksekusi BUY/SELL)
```

## Cara Membuka Project

1. Buka folder ini di **Android Studio** (Koala/Ladybug 2024.x atau lebih
   baru, dengan AGP 8.6+ & Kotlin 2.0.20).
2. Isi Gemini API key kamu lewat screen **Pengaturan** setelah app jalan
   (tersimpan terenkripsi, tidak pernah masuk source code).
3. Jalankan target `app` di emulator/device Android 10+.

## Roadmap Selanjutnya (Belum Dikerjakan)

- Perbaikan hasil compile pertama kali (lihat peringatan di atas).
- Certificate pinning nyata.
- Migration Room eksplisit.
- UI pemilihan bahasa manual di Settings.
- `AccountRepository` nyata (jika diputuskan ingin baca data Exness otomatis).
- Unit test (domain layer sudah pure Kotlin, siap ditest — tinggal ditulis).
- Signing config + rilis internal (closed testing).

Detail lengkap tiap fase & keputusan arsitektur ada di `docs/Blueprint_v0.md`.
"# tradingpilot_ai" 
