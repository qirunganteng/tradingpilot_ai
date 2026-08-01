# UI_TradePilot_Modular

Paket ini berisi **seluruh file UI Browser** TradePilot AI Desktop yang sudah
di-refactor sesuai 15 prioritas di prompt kamu, dikemas modular per-folder
supaya kamu bisa ubah tampilan tanpa menyentuh backend/AI Gateway/logic
trading.

## Baca ini dulu: apa yang benar-benar ada di paket ini

Paket sumber (`TradePilotAI-BrowserFiles.zip`) yang kamu upload **hanya
berisi modul Browser murni** — file `CopilotPanel.kt` (AI Panel/Risk
Calculator/Chart Analysis) dan `SettingsDialog.kt`/`DesktopSettingsStore.kt`
**tidak disertakan** di paket itu. Karena itu:

| Prioritas | Status |
|---|---|
| 1. Title bar custom | ✅ Selesai — `Window/` |
| 2. Sidebar 7 icon aktif | ✅ Selesai — `ActivityBar/` |
| 3. Explorer panel | ✅ Selesai — `Explorer/` (Downloads = stub jujur, lihat catatan di file) |
| 4. Compact toolbar | ✅ Selesai — `Toolbar/`, `AddressBar/`, `Theme/Dimens.kt` |
| 5. AI Panel compact | ⚠️ **Tidak bisa dikerjakan** — file panel-nya tidak ada di paket sumber. Lihat `AIWorkspace/NOT_INCLUDED.md` |
| 6. Browser Menu | ✅ Selesai (sebagian item stub jujur) — `BrowserMenu/` |
| 7. Risk Calculator | ⚠️ **Tidak bisa dikerjakan** — bukan bug UI, ini logic/network. Lihat `RiskCalculator/NOT_INCLUDED.md` |
| 8. Analisa Chart | ⚠️ **Tidak bisa dikerjakan** — sama seperti di atas. Lihat `ChartAnalysis/NOT_INCLUDED.md` |
| 9. Fullscreen browser | ✅ Selesai — `Fullscreen/` |
| 10. Browser UX (shortcut, middle-click) | ✅ Selesai — `Components/KeyboardShortcuts.kt` |
| 11. Multi Tab (pin/mute/duplicate/reorder) | ✅ Selesai — `TabBar/` |
| 12. Loading/Error/Offline page | ✅ Selesai — `BrowserPanel/` |
| 13. UI Polish | ✅ Selesai — `Theme/`, `Animation/` |
| 14. Jangan ubah backend | ✅ Dipatuhi — lihat tabel dependency di bawah |

Beberapa item di dalam yang ✅ tetap punya sub-bagian yang **stub jujur**
(ditandai jelas di kode dengan komentar "CATATAN JUJUR") karena butuh
perubahan di luar scope modul UI murni (mis. New Window, Incognito, Print,
Clear Browsing Data, Downloads real). Jangan anggap semua yang tertulis ✅
100% fungsional end-to-end — baca komentar di file terkait.

## Struktur folder & fungsinya

```
UI_TradePilot_Modular/
├── Main.kt                    -- entry point (fun main())
├── Window/                    -- Prioritas 1: window custom, title bar
├── Theme/                     -- warna & ukuran terpusat (Prioritas 13)
├── Animation/                 -- durasi animasi terpusat (Prioritas 13)
├── ActivityBar/                -- Prioritas 2: sidebar kiri 7 icon
├── Explorer/                   -- Prioritas 3: panel Explorer/History/Bookmarks/Downloads/Workspace
├── Toolbar/                     -- Prioritas 4: BrowserBar (nav + address + menu button)
├── AddressBar/                  -- Prioritas 4: address field + tombol bookmark
├── TabBar/                      -- Prioritas 11: model tab + tab bar (pin/mute/duplicate/reorder)
├── BrowserMenu/                 -- Prioritas 6: menu Chrome-style
├── BrowserPanel/                -- render JCEF + JCEF engine + bootstrap (loading/error/offline)
├── Fullscreen/                  -- Prioritas 9: auto-hide chrome saat fullscreen
├── Components/                  -- FindBar (Ctrl+F) + KeyboardShortcuts (Prioritas 10)
├── Layouts/                     -- Workbench.kt (perakit semua modul) + VerticalResizeHandle
├── Icons/                       -- catatan pemakaian Material Icons (bukan drawable)
├── Drawable/, Resources/, Styles/  -- NOT_APPLICABLE.md (konsep Android, tidak berlaku di Compose Desktop)
├── AIWorkspace/, RiskCalculator/, ChartAnalysis/ -- NOT_INCLUDED.md (file sumbernya tidak ada di paket asal)
└── shared/                       -- BrowserEngine interface (ditambah find/stopFind) + BrowserConstants (utuh)
```

## File mana yang AMAN dimodifikasi bebas

Semua file di atas **kecuali** yang disebut di bagian "berhubungan dengan
backend" di bawah. Yang paling sering kamu sentuh kalau mau ubah tampilan:

- `Theme/Theme.kt`, `Theme/Dimens.kt` — ubah warna/ukuran, otomatis kepakai
  di semua komponen karena semua import dari sini.
- `Toolbar/BrowserBar.kt`, `AddressBar/AddressBar.kt` — tampilan toolbar.
- `ActivityBar/ActivityBar.kt`, `Explorer/ExplorerPanel.kt` — sidebar kiri.
- `TabBar/BrowserTabsBar.kt` — tampilan tab.

## File yang BERHUBUNGAN dengan backend/logic (hati-hati, tapi TETAP di paket ini karena juga bagian UI)

- `BrowserPanel/JCEFBrowserEngine.kt` & `BrowserPanel/JCEFBootstrap.kt` —
  ini "jembatan" ke Chromium Embedded Framework. Boleh diubah, tapi
  README asli menandai keduanya "paling rawan" — kalau ubah, tes betul-betul
  siklus buka/tutup tab & aplikasi (memory leak proses CEF child gampang
  muncul kalau lifecycle-nya salah).
- `shared/BrowserEngine.kt` — interface yang dipakai juga (kemungkinan) oleh
  android-client. Method baru (`find`/`stopFind`) dikasih default
  implementation kosong supaya tidak wajib langsung diimplementasikan di
  android-client.

## File yang TIDAK diikutkan sama sekali (murni backend, di luar paket ini)

`AI Gateway`, `Database`, `Worker`, `Cloudflare`, `Authentication`, `Business
Logic Trading`, `CopilotPanel.kt`, `SettingsDialog.kt`,
`DesktopSettingsStore.kt` — semua ini dipanggil dari `Layouts/Workbench.kt`
persis seperti pemanggilan aslinya (import & parameter sama persis), TAPI
isi filenya sendiri tidak ada dan tidak disentuh sama sekali.

## Dependency antar file (ringkas)

```
Main.kt
 └─ Window/AppWindow.kt (undecorated Window)
     └─ Window/CustomTitleBar.kt (title bar + drag/min/max/close)
         └─ Layouts/Workbench.kt   <-- pusat perakitan
             ├─ ActivityBar/ActivityBar.kt
             ├─ Explorer/ExplorerPanel.kt
             │   ├─ Explorer/HistoryStore.kt
             │   └─ Explorer/BookmarkStore.kt
             ├─ Layouts/VerticalResizeHandle.kt
             ├─ Fullscreen/FullscreenController.kt (FullscreenRevealHost)
             │   ├─ TabBar/BrowserTabsBar.kt (+ TabBar/BrowserTab.kt)
             │   ├─ Toolbar/BrowserBar.kt (+ AddressBar/AddressBar.kt)
             │   ├─ BrowserMenu/BrowserMenu.kt
             │   ├─ BrowserPanel/JCEFBrowserView.kt
             │   │   └─ BrowserPanel/JCEFBrowserEngine.kt
             │   │       └─ BrowserPanel/JCEFBootstrap.kt
             │   └─ Components/FindBar.kt
             ├─ Components/KeyboardShortcuts.kt (dipasang di root Box)
             ├─ com.tradepilot.desktop.copilot.CopilotPanel   <-- TIDAK di paket ini
             └─ com.tradepilot.desktop.settings.SettingsDialog <-- TIDAK di paket ini

Semua warna/ukuran datang dari Theme/Theme.kt & Theme/Dimens.kt
Semua BrowserEngine (JCEF) mengimplementasikan shared/BrowserEngine.kt
```

## Alur UI ringkas

1. `main()` di `Main.kt` membuka `AppWindow` (undecorated, title bar custom).
2. `Workbench` merender: `ActivityBar` (kiri) → `ExplorerPanel` (kalau bukan
   fullscreen) → `Workspace` (tengah, isinya TabsBar+BrowserBar+JCEFBrowserView,
   dibungkus `FullscreenRevealHost`) → `CopilotPanel` (kanan, kalau visible).
3. Klik icon di `ActivityBar` mengubah `activePanel` (state di `Workbench`)
   → `ExplorerPanel` render ulang sesuai panel yang dipilih.
4. Navigasi (address bar / klik item Explorer / quick-link) selalu lewat
   `JCEFBrowserEngine.loadUrl(...)`, lalu `JCEFBrowserEngine` broadcast balik
   `addressState`/`titleState` yang dipakai `Workbench` buat sinkron tab aktif
   + catat ke `HistoryStore`.
5. Semua keyboard shortcut (Ctrl+T dkk) ditangkap satu pintu di
   `Components/KeyboardShortcuts.kt`, dipasang di root `Box` milik
   `Workbench`.

## Cara pasang balik ke project asli

1. Copy isi tiap folder ke lokasi package Kotlin yang sesuai, mis.:
   - `ActivityBar/ActivityBar.kt` → `desktop-client/.../kotlin/com/tradepilot/desktop/activitybar/ActivityBar.kt`
   - `Explorer/*.kt` → `.../com/tradepilot/desktop/explorer/`
   - dst — package declaration di baris pertama tiap file SUDAH benar,
     tinggal ikuti itu untuk menentukan folder tujuan (folder kapital di
     paket ini cuma pengelompokan sesuai permintaan prompt, bukan nama
     package Kotlin asli).
2. `shared/BrowserEngine.kt` & `shared/BrowserConstants.kt` → module
   `:shared` (`.../domain/browser/`).
3. **Ganti isi lama** `Main.kt` di project asli dengan `Main.kt` di paket
   ini (fungsinya sama, cuma manggil `AppWindow` bukan `Window` langsung).
4. Kalau project asli sudah punya `CopilotPanel.kt`/`SettingsDialog.kt`/
   `DesktopSettingsStore.kt` — JANGAN dihapus/ditimpa, `Layouts/Workbench.kt`
   di paket ini memanggilnya apa adanya (import path harus cocok dengan yang
   sudah ada di project kamu; sesuaikan kalau package-nya beda).
5. Build & jalankan; cek satu-satu daftar ✅/⚠️ di tabel atas.

## Batasan jujur yang perlu kamu tahu (ringkasan dari catatan di tiap file)

- **Multi-tab** memakai SATU JCEF engine bergantian, bukan tab paralel
  sungguhan (detail: `TabBar/BrowserTab.kt`).
- **Mute tab** murni state UI, tidak benar-benar membisukan audio (JCEF
  versi ini tidak expose kontrol itu).
- **Downloads** panel sudah siap tampilannya tapi belum ada
  `CefDownloadHandler` yang disambungkan — jujur ditampilkan sebagai
  status kosong, bukan data palsu.
- **Loading progress bar** simulasi (CEF tidak expose persentase asli),
  bukan progress byte-per-byte.
- **New Window / Incognito / Print / Clear Browsing Data** di Browser Menu
  masih stub (`TODO` di kode) — butuh perubahan arsitektur JCEF yang lebih
  besar (CefRequestContext terpisah, dst).
- **DevTools** best-effort (`browser.openDevTools()`) — tergantung versi
  JCEF yang dipakai project, dibungkus try-catch supaya tidak crash kalau
  tidak didukung.
