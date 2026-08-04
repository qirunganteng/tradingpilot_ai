# Release Checklist — TradePilot AI Beta

Checklist ini dipisah per area supaya jelas siapa yang bertanggung jawab
mengecek apa. Centang manual sebelum rilis ke closed testing.

## 1. Kesiapan Fungsional (Android App)

- [x] Semua 10 fitur (Browser, AI Analysis, Money Management, Journal + Add Trade, Statistic, Notification/Copilot, Mentor, Settings, Drawing/Annotation, Screenshot) punya UI dan logic yang saling terhubung.
- [x] Alur inti (tekan ANALISA → hasil tampil) tersambung end-to-end sampai ke Worker.
- [x] Gap fungsional "tidak ada cara isi Journal" sudah ditutup (`AddTradeScreen`).
- [ ] **Belum ada**: history sync dari Worker (`/api/v1/analyses`) ditampilkan di app — saat ini endpoint sudah ada di backend tapi belum dipakai UI manapun. Opsional untuk beta pertama, tapi dicatat sebagai gap.
- [ ] **Belum ada**: pemilihan bahasa manual (ikut bahasa perangkat saja) — dicatat sebagai keterbatasan sejak Fase 0.
- [ ] Launcher icon masih placeholder vector sederhana — ganti dengan logo final sebelum rilis publik (build debug/internal testing tidak masalah pakai placeholder).

## 2. Keamanan

- [x] API key Gemini **tidak pernah** disimpan di device (arsitektur Worker Gateway memindahkannya ke server).
- [x] Kalau fallback `GeminiProvider` langsung tetap dipakai, API key tersimpan di `EncryptedSharedPreferences`, bukan plaintext.
- [x] Gateway token dikirim lewat header, bukan query string (tidak ter-log di access log URL).
- [x] `HttpLoggingInterceptor` level `BASIC` (bukan `BODY`) — payload/header sensitif tidak ter-log ke Logcat.
- [x] `FLAG_SECURE` aktif di screen Money Management (cegah screenshot data finansial).
- [x] Root detection dengan warning (bukan block total, supaya developer/tester tetap bisa pakai).
- [x] WebView di-hardening: `allowFileAccess`, `allowUniversalAccessFromFileURLs` dimatikan; tidak ada `addJavascriptInterface` yang expose kontrol trading.
- [x] Worker: auth token wajib untuk semua endpoint kecuali `/health`; rate limit per device; input validation (`imageBase64` wajib).
- [ ] **TODO sebelum rilis publik luas**: certificate pinning nyata (hook sudah ada di `NetworkModule`/`WorkerModule`, tinggal isi hash sertifikat).
- [ ] **TODO sebelum rilis publik luas**: ganti `GATEWAY_AUTH_TOKEN` statis dengan token per-install atau Play Integrity API (token statis cukup untuk beta tertutup, kurang untuk publik).
- [ ] **TODO**: Room migration eksplisit (saat ini `fallbackToDestructiveMigration()` — aman untuk beta karena data lokal, tapi HARUS diganti sebelum user beta kehilangan data journal mereka di update berikutnya).

## 3. Kenyamanan Pengguna (Android/HP)

- [x] Dark mode default, tema konsisten ala code-editor (sesuai preferensi awal).
- [x] Font monospace untuk semua data numerik (harga, lot, RR) — mudah dibaca cepat.
- [x] Loading state jelas di semua screen async (Analysis, Journal, dll — bukan layar kosong/freeze).
- [x] Pesan error dalam Bahasa Indonesia yang jelas (bukan stack trace teknis) di semua alur utama.
- [x] Disclaimer "AI tidak bertransaksi" tampil permanen di status bar bawah.
- [x] Downscale gambar otomatis (hemat kuota data seluler user, lihat `docs/PERFORMANCE.md`).
- [ ] **Belum diverifikasi**: kenyamanan real di device kecil (layar <6 inch) — Activity Bar + Bottom Panel belum dites di berbagai ukuran layar nyata (butuh device fisik/emulator beragam).
- [ ] **Belum ada**: pull-to-refresh atau indikator "data terakhir diperbarui kapan" di Journal/Statistic.

## 4. Kenyamanan "PC" (Browser & Dashboard)

Karena TradePilot AI adalah aplikasi Android (bukan aplikasi web/desktop),
"pengalaman PC" yang relevan adalah dua hal:

- [x] **Health check Worker via browser PC** — `https://<worker-url>/api/v1/health` bisa dibuka langsung di browser apa pun (Chrome/Edge/Firefox di PC), menampilkan JSON status tanpa perlu tool tambahan.
- [x] **Dashboard Cloudflare** (D1 browser, R2 bucket viewer, Worker logs real-time) — semuanya built-in di dashboard Cloudflare, otomatis responsive di browser PC, tidak perlu setup tambahan.
- [x] **GitHub Actions tab** — bisa dipantau dari browser PC maupun HP (GitHub web responsive), lihat `docs/DEPLOY.md` Bagian C.4.
- [ ] **Kalau nanti dibutuhkan**: dashboard admin custom (mis. lihat semua histori analisa lintas-device dalam 1 halaman) — belum dibuat, di luar scope beta pertama. Endpoint `/api/v1/analyses` sudah siap dipakai kalau mau dibuatkan front-end sederhana nanti (bisa HTML statis + fetch, di-hosting di Cloudflare Pages).

## 5. Sebelum Menekan "Deploy ke Publik"

Urutan yang disarankan:

1. Jalankan seluruh checklist manual QA di `docs/TESTING.md` bagian 4.
2. Selesaikan semua item "TODO sebelum rilis publik luas" di bagian Keamanan di atas.
3. Ganti `fallbackToDestructiveMigration()` jadi migration eksplisit begitu skema Room dianggap stabil.
4. Distribusikan dulu ke **closed testing** (Firebase App Distribution / Play Internal Testing) minimal ke beberapa tester nyata selama 1-2 minggu.
5. Kumpulkan feedback, perbaiki, baru pertimbangkan rilis lebih luas.

Checklist ini hidup — update terus seiring fitur baru ditambahkan.
