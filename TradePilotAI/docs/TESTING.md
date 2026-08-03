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

## 5. Desktop Client — Browser Engine (JCEF) — teruji sebagian (fungsi pure), sisanya manual

Dikerjakan sebagai bagian dari audit "FASE 1 -- Browser Engine Foundation"
(lihat `D:\rule tradepilot ai claude\masterprompt.txt`). Sama seperti Android
di atas: hanya fungsi PURE yang bisa di-unit-test tanpa native binary CEF
ter-load (butuh koneksi internet first-run + konteks AWT/window sungguhan --
tidak realistis untuk unit test). Jalankan dengan:

```
cd desktop-client
gradlew.bat :app:test
```

**Hasil sungguhan (bukan simulasi) -- dijalankan lewat Gradle asli di mesin
Windows, BUILD SUCCESSFUL:**
- `NormalizeUrlTest` -- 15/15 PASS. Cakupan: skema yang sudah ada (http/
  https/about/data/custom) dikembalikan apa adanya; domain/subdomain/IPv4/
  localhost (dengan/tanpa port) ditambah `https://`; teks yang BUKAN alamat
  web (kata polos, frasa dengan spasi, karakter spesial) diarahkan ke
  pencarian Google alih-alih dicoba jadi domain literal (regresi utk bug
  "ngetik kata biasa -> DNS_PROBE_FINISHED_NXDOMAIN" yang sempat kejadian).
- `KeyboardShortcutsNativeTest` -- 16/16 PASS. Cakupan: semua shortcut
  (`Ctrl+T/W/L/R/F/D`, `Ctrl+Shift+T`, `Ctrl+Tab`, `F5`, `F11`, `Alt+Left/
  Right`) memanggil aksi yang benar TEPAT SATU KALI; `ESC` hanya keluar
  fullscreen kalau memang sedang fullscreen (tidak boleh dua arah seperti
  F11); kombinasi tombol tak dikenal tidak salah nyantol ke shortcut lain.

**Yang BELUM diuji otomatis (butuh CefBrowser/CefClient native sungguhan --
unit test murni tidak realistis untuk ini):**
- Multi-window (`New Window`/`New Incognito Window`) benar-benar dapat
  `CefClient` terpisah (bukan cuma lolos compile) -- perbaikan bug
  "handler cuma nyantol ke window pertama" di JCEFBootstrap.kt.
- Crash Recovery (`onRenderProcessTerminated`) benar-benar muncul Error
  Page saat render process di-kill paksa.
- `target="_blank"`/`window.open()` benar-benar navigasi di tab yang sama,
  bukan buka popup native OS.
- Focus Management: shortcut (`Ctrl+T` dkk) benar-benar tetap jalan saat
  kursor sedang di DALAM halaman web (bukan cuma saat fokus di address bar).
- Sinkronisasi fullscreen 2 arah (video/chart HTML5 fullscreen <-> fullscreen
  aplikasi) + `ESC` menutup keduanya sekaligus.
- `cache_path` benar-benar persisten di `~/.tradepilot/jcef-cache` setelah
  restart app.
- JS Bridge (`window.tradePilotQuery(...)`) benar-benar terpanggil dari
  DevTools console halaman manapun (lihat contoh perintah test di komentar
  `JCEFBrowserEngine.kt`).

### Checklist Manual Desktop (jalankan `gradlew.bat :app:run`, lalu coba satu-satu)

- [ ] Buka "New Window" dari menu browser -- address bar/title/back-forward
      window KEDUA ikut update saat browsing (bukan cuma window pertama).
- [ ] Buka "New Incognito Window" -- browsing di sana, tutup, cek folder
      `~/.tradepilot/jcef-cache` tidak bertambah jejak baru (isolasi cache,
      catatan: cookie kemungkinan lebih terisolasi daripada cache -- lihat
      komentar kode).
- [ ] Klik link yang biasanya buka tab baru (`target="_blank"`, mis. tombol
      share/auth di banyak situs) -- harus navigasi di tab yang sama, BUKAN
      buka window OS terpisah tanpa chrome.
- [ ] Klik ke dalam konten halaman (fokus masuk ke browser native), lalu
      coba `Ctrl+T`, `Ctrl+W`, `Ctrl+F`, `F11` -- semua harus tetap bereaksi
      (sebelum fix, ini semua diam begitu fokus masuk ke halaman).
- [ ] Putar video yang bisa fullscreen (atau buka chart TradingView, klik
      fullscreen) -- chrome aplikasi (tab bar/address bar) ikut hilang
      otomatis; tekan `ESC` -- video/chart KELUAR fullscreen DAN chrome
      aplikasi muncul kembali dalam satu tombol.
- [ ] Dari DevTools console halaman manapun, jalankan:
      `window.tradePilotQuery({request: "ping", onSuccess: r => console.log(r), onFailure: (c,m) => console.error(c,m)})`
      (setelah pasang `engine.onJsBridgeQuery` sisi Kotlin untuk uji coba) --
      harus muncul response di console, bukan hang selamanya.
- [ ] Restart app -- cek folder `~/.tradepilot/jcef-cache` tetap ada/dipakai
      ulang (bukan dibuat baru tiap start).

### FASE 2 -- Tab Manager & Window Manager (checklist manual tambahan)

- [ ] Buka beberapa tab ke situs berbeda, scroll salah satu ke tengah
      halaman, isi form (belum submit) di tab lain, pindah-pindah tab --
      scroll position & isi form HARUS tetap ada saat balik ke tab itu
      (beda dari FASE 1: dulu semua ini hilang tiap ganti tab).
- [ ] Klik kanan tab -> Reload salah satu tab yang **sedang TIDAK aktif** --
      harus benar-benar reload (dulu cuma tab aktif yang bisa).
- [ ] Klik link yang biasanya login OAuth (Google/Facebook sign-in di
      situs mana pun) -- popup harus muncul sebagai window terpisah
      sungguhan (bukan navigasi tab yang sama), dan setelah login selesai
      tab pemicu harus tetap di halaman asal (bukan ikut ter-navigasi).
- [ ] Tutup app dengan beberapa tab & window terbuka (bukan incognito),
      buka lagi -- window & tab yang sama harus muncul kembali (Browser
      Session/Restore).
- [ ] Buka window Incognito, browsing, tutup app sepenuhnya, buka lagi --
      window incognito TIDAK BOLEH ikut ter-restore (privasi).
- [ ] Geser/resize window ke ukuran & posisi tertentu, tutup app, buka
      lagi -- window harus muncul di ukuran/posisi yang sama (Window State).
- [ ] Buka "New Window" tanpa pernah resize sebelumnya -- ukurannya harus
      ikut ukuran terakhir yang dipakai (bukan selalu 1280x800 baku).
