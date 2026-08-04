# Optimasi Performa — TradePilot AI

Dokumen ini mencatat optimasi konkret yang sudah diterapkan di kode (bukan
sekadar saran), plus rekomendasi lanjutan yang belum diimplementasikan.

## Sudah Diterapkan

| # | Optimasi | Lokasi | Dampak |
|---|---|---|---|
| 1 | Downscale gambar (maks lebar 1280px) sebelum compress JPEG | `feature-screenshot/ScreenCapture.kt` | Mengurangi ukuran payload ~60-80% pada device resolusi tinggi (mis. 1440p→1280px), mempercepat upload & mengurangi pemakaian data seluler user, tanpa menurunkan kualitas analisa AI (candle/level harga tetap terbaca jelas) |
| 2 | `LAYER_TYPE_SOFTWARE` sesaat saat capture WebView | `feature-screenshot/ScreenCapture.kt` | Mencegah bitmap kosong/hitam akibat WebView hardware-accelerated; dikembalikan ke layer asli segera setelah capture supaya scrolling/pan chart tetap smooth |
| 3 | Timeout eksplisit OkHttp (connect 10s, write 30s, read 45s) + `retryOnConnectionFailure` | `core-network/NetworkModule.kt`, `data-ai/WorkerModule.kt` | Default OkHttp (10s semua) terlalu pendek untuk panggilan AI multimodal (upload gambar + inference Gemini); sekarang gagal cepat untuk masalah koneksi tapi punya waktu cukup untuk request yang memang butuh lama |
| 4 | Index Room pada kolom `timestamp` (`trade_history`, `notification_log`) | `core-database/entity/Entities.kt` | Query `ORDER BY timestamp DESC` (dipakai Journal & Notification Center) tidak perlu full table scan seiring data bertambah |
| 5 | Rate limit di Worker (10 req/menit/device, KV-based) | `cloudflare-worker/src/rateLimit.ts` | Mencegah 1 device menghabiskan kuota Gemini API secara tidak sengaja (mis. bug loop di Copilot polling) |
| 6 | `OkHttpClient`/`Retrofit`/D1-DAO semua di-scope `@Singleton` lewat Hilt | seluruh `core-network`, `data-ai`, `core-database` | Connection pool OkHttp dipakai bersama, tidak dibuat ulang per request — mengurangi overhead TLS handshake berulang |
| 7 | Audit log (D1) & rate-limit check bersifat *best-effort*, tidak memblokir response sukses | `cloudflare-worker/src/index.ts` | Latensi tambahan dari D1 write tidak menambah waktu tunggu user secara signifikan, dan kegagalan D1 tidak menggagalkan fitur inti |

## Rekomendasi Lanjutan (Belum Diimplementasikan)

Diurutkan berdasarkan dampak vs effort:

1. **Cache hasil analisa duplikat di Worker.** Kalau user menekan ANALISA dua kali untuk chart yang identik dalam rentang singkat, Worker bisa cek hash gambar (mis. simpan hash SHA-256 8 karakter pertama sebagai KV key) sebelum panggil Gemini lagi — hemat kuota & latensi instan untuk kasus duplikat.
2. **Streaming response dari Gemini** (`streamGenerateContent` alih-alih `generateContent`) supaya user lihat hasil parsial lebih cepat, alih-alih menunggu seluruh response selesai. Butuh perubahan signifikan di UI (AnalysisScreen perlu render incremental) — cocok untuk fase berikutnya, bukan quick win.
3. **Lifecycle policy R2** (auto-hapus objek setelah N hari) via Cloudflare R2 Object Lifecycle Rules (dikonfigurasi di dashboard Cloudflare, bukan kode) — supaya biaya storage tidak menumpuk tanpa batas untuk screenshot lama yang sudah tidak relevan.
4. **Compose recomposition audit.** Belum diverifikasi dengan Layout Inspector/Compose Compiler Metrics (butuh Android Studio) apakah ada recomposition berlebihan di `AnalysisScreen`/`JournalScreen` saat state berubah — masuk checklist QA manual di `docs/TESTING.md`.
5. **WorkManager untuk retry upload gagal** — saat ini kalau upload ke Worker gagal (mis. app di-background saat network putus), hasil analisa hilang begitu saja. Bisa dipindah ke WorkManager job yang retry otomatis.
6. **Index device_id di D1** — saat ini hanya composite index `(device_id, created_at)`; cukup untuk skala kecil-menengah, tapi kalau jumlah user besar, pertimbangkan partisi tabel per-bulan.

## Cara Verifikasi Klaim di Atas

Klaim performa di tabel "Sudah Diterapkan" adalah **klaim arsitektural yang masuk akal secara teori** (downscale gambar pasti mengurangi ukuran file, index pasti mempercepat query terurut) — tapi **belum diukur dengan profiler sungguhan** di device Android nyata (butuh Android Studio Profiler) karena keterbatasan lingkungan kerja saat ini. Setelah kamu build APK pertama kali, disarankan ukur baseline dengan:

- **Waktu end-to-end tombol ANALISA** (tap → hasil tampil) — target wajar: <5 detik di koneksi WiFi normal.
- **Ukuran payload upload** — cek lewat `HttpLoggingInterceptor` (sudah aktif, level `BASIC`) di Logcat, filter tag `OkHttp`.
- **Cold start time** — target dari Blueprint awal: <2 detik (Blueprint bagian "Performance").
