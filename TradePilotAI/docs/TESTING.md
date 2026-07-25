# Testing — TradePilot AI

Ringkasan semua pengujian yang **benar-benar dijalankan** (bukan cuma
ditulis) selama pengembangan, plus yang masih perlu kamu jalankan manual
karena keterbatasan lingkungan kerja (tidak ada Android SDK/device).

## 1. Cloudflare Worker — teruji penuh, otomatis

```bash
cd cloudflare-worker
npm install
npm test        # Vitest — 14/14 PASS (unit + integrasi lewat vitest-pool-workers)
npm run test:e2e  # Miniflare langsung — 15/15 PASS (D1 & R2 byte-level, rate limit, auth)
```

Cakupan:
- `PromptBuilder` (konsistensi prompt ICT/SMC).
- Auth (token benar/salah/kosong → 401).
- Validasi input (`imageBase64` kosong → 400).
- Alur analisa penuh: Gemini (di-mock) → parsing JSON → simpan D1 → simpan R2 → response.
- Signal tidak dikenali → fallback `NONE` (bukan crash).
- Error Gemini (mis. rate limit dari Google) → diteruskan sebagai error terstruktur, bukan 500 generik.
- History endpoint hanya mengembalikan data device yang diminta (isolasi antar device).
- Rate limit internal (429 setelah 10 request/menit/device).

**Catatan jujur**: `npm test` melewati test khusus R2 karena bug tooling di
`@cloudflare/vitest-pool-workers` v0.5.41 (bukan bug kode kita — lihat
`test/R2_TESTING_NOTE.md`). Verifikasi R2 tetap dilakukan nyata lewat
`npm run test:e2e` yang jalan di Miniflare langsung tanpa lapisan yang
bermasalah itu.

## 2. Domain Layer (Android, business logic) — teruji penuh

Karena lingkungan awal tidak punya Android SDK/Gradle, logika inti
(`CalculateRiskUseCase`, `CalculateJournalStatisticsUseCase`,
`GenerateHistoryInsightUseCase`, `GenerateMentorFeedbackUseCase`,
`DeriveCopilotSignalUseCase`) diverifikasi dengan **Kotlin compiler asli**
(bukan simulasi) + 32 assertion custom — semua PASS. Hasil ini disalin
menjadi unit test JUnit resmi di `domain/src/test/java/...` supaya bisa
dijalankan ulang dengan:

```bash
./gradlew :domain:test
```

Atau, tanpa perlu Gradle/Android Studio sama sekali (dipakai di CI sebagai
gerbang cepat sebelum build Android yang berat):

```bash
./scripts/verify-domain-logic.sh
```

## 3. Android — Compile-check Statis (bukan compile Gradle penuh)

Lihat `docs/AUDIT.md` untuk detail metodologi. Ringkasan: 73 file `.kt`,
22 module Gradle, semua konsisten secara struktural. 3 bug nyata ditemukan
& diperbaiki selama proses ini (icon extended, dependency coroutines,
dependency `core-security` di `data-ai`).

## 4. Yang BELUM Diuji (Butuh Android Studio/Device Nyata)

Ini bukan kelalaian — murni karena lingkungan kerja tidak punya Android
SDK, emulator, atau device fisik. **Checklist manual QA** di bawah ini
WAJIB dijalankan minimal sekali sebelum rilis beta:

### Checklist Manual E2E (jalankan di device/emulator Android 10+)

**Setup awal**
- [ ] `./gradlew assembleDebug` sukses tanpa error.
- [ ] App terbuka, tidak crash di splash/first launch.
- [ ] Isi konfigurasi Worker (URL + gateway token) di Settings, cek status berubah jadi "terkonfigurasi".

**Alur inti (Browser → Analisa AI)**
- [ ] WebView berhasil load `my.exness.com/webtrading`.
- [ ] Toolbar (Back/Forward/Refresh) berfungsi.
- [ ] Login manual ke akun Exness berhasil di dalam WebView.
- [ ] Tekan tombol ANALISA → berpindah ke screen Analisa, muncul loading, lalu hasil (atau pesan error yang jelas kalau Worker belum dikonfigurasi/gagal).
- [ ] Hasil analisa menampilkan pair, trend, signal, confidence, entry/SL/TP/RR, alasan — semua terisi (bukan "N/A" semua, kecuali API key/Worker memang belum diisi).
- [ ] Cek dashboard Cloudflare (D1 & R2) — record baru muncul di tabel `analyses`, file gambar baru muncul di bucket R2.

**Money Management**
- [ ] Isi balance/risk%/entry/SL/TP manual → tekan Hitung → hasil lot/RR/max daily loss muncul dan masuk akal.
- [ ] Coba screenshot screen ini (harus GAGAL / layar hitam karena `FLAG_SECURE` — ini perilaku BENAR, bukan bug).

**Journal & Statistik**
- [ ] (Belum ada UI "tambah trade manual" — perlu ditambahkan sebelum QA ini bisa lengkap; lihat `docs/RELEASE_CHECKLIST.md` poin gap fitur.)
- [ ] Setelah ada data trade, cek winrate/profit factor/average RR di Journal masuk akal.
- [ ] Statistik menampilkan progress bar sebelum 100 trade, insight lengkap setelahnya.

**AI Copilot / Notification**
- [ ] Tekan "Mulai Monitor" di Notification Center → muncul log baru dalam ~60 detik (interval default) kalau ada sinyal.
- [ ] Tekan "Hentikan Monitor" → tidak ada log baru lagi setelahnya.
- [ ] Pindah ke screen lain lalu kembali → status monitor tidak menyala sendiri (harus manual on/off).

**AI Mentor**
- [ ] Feedback muncul per trade yang tersimpan di Journal, sesuai kondisi (profit/rugi/SL dekat/dll).

**Keamanan**
- [ ] Root warning muncul di device root (test di emulator dengan root, atau skip kalau tidak ada).
- [ ] API key & gateway token TIDAK terlihat di Logcat (grep log untuk memastikan tidak ter-log di HttpLoggingInterceptor level BODY — kita pakai level BASIC justru untuk ini).
- [ ] Uninstall app → reinstall → API key/config hilang (EncryptedSharedPreferences terikat ke app data, ini perilaku benar).

**Performa (lihat juga `docs/PERFORMANCE.md`)**
- [ ] Cold start < 2 detik (stopwatch manual).
- [ ] Waktu tombol ANALISA sampai hasil tampil < 5 detik di WiFi normal.
- [ ] Scroll/pan chart di WebView tetap smooth (tidak patah-patah) walau capture dipakai berulang kali.

Setelah checklist ini lolos, app siap masuk ke closed testing (lihat
`docs/RELEASE_CHECKLIST.md`).
