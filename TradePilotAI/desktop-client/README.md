# TradePilot AI — Desktop Client

Workbench trading lintas platform ala VS Code (Compose Desktop + JCEF).
Lihat `CONSTITUTION.md` (root TradePilotAI) untuk visi & aturan arsitektur
lengkap.

## Menjalankan versi jadi (tanpa install apapun)

Download `TradePilotAI-Desktop-Windows.zip` dari:
https://github.com/qirunganteng/tradingpilot_ai/releases/tag/latest

Extract, lalu jalankan `TradePilot AI.exe` di dalamnya. JRE sudah
dibundel di situ (hasil `createDistributable`), jadi **tidak perlu
install Java**. First run bisa agak lama karena JCEF (browser engine)
mengunduh binary native-nya sendiri saat pertama kali jalan.

## Menjalankan dari source (butuh JDK 17)

```bash
./gradlew.bat :app:run
```

## Build distributable sendiri

```bash
./gradlew.bat :app:createDistributable
# hasil: app/build/compose/binaries/main/app/TradePilot AI/
```

## Konfigurasi AI Gateway

Buka aplikasi -> ikon gear (Settings) di ActivityBar -> isi Gateway URL
& Auth Token (nilai yang sama dengan `GATEWAY_AUTH_TOKEN` di
`backend/cloudflare-worker`). Tersimpan terenkripsi di
`~/.tradepilot/desktop-client.properties` (lihat
`app/src/main/kotlin/com/tradepilot/desktop/settings/DesktopCrypto.kt`
untuk detail & batasannya).

Alternatif (buat CI/dev script): set environment variable
`TRADEPILOT_GATEWAY_URL` dan `TRADEPILOT_GATEWAY_TOKEN` -- ini
diprioritaskan di atas Settings panel.

## Browser UI — Status Refactor (ref: `buat_claude.txt`)

Refactor ini **hanya menyentuh modul UI Browser** (window, title bar,
activity bar, explorer, toolbar, AI panel, browser menu, tab bar,
fullscreen, shortcut). **Tidak ada** perubahan di backend, AI Gateway,
database, atau JCEF lifecycle (`JCEFBootstrap.kt` / inti
`JCEFBrowserEngine.kt`) — sesuai batasan di Prioritas 14.

| # | Prioritas | Status | Catatan |
|---|-----------|--------|---------|
| 1 | Hapus title bar putih Windows | ✅ Selesai | `window/AppWindow.kt` (`undecorated = true`) + `window/CustomTitleBar.kt` (drag, minimize, maximize/restore, close, background `#1E1E1E`). |
| 2 | Sidebar kiri — semua icon aktif | ✅ Selesai | `activitybar/ActivityBar.kt`: Explorer/History/Bookmark/Downloads/Workspace/AI/Settings semua bisa diklik, ada hover, active-state (garis biru kiri ala VSCode), click-animation (scale), dan **tooltip** (barusan diperbaiki — sebelumnya `TooltipBox` sempat di-import tapi tidak dipakai). |
| 3 | Explorer Panel (VSCode-style) | ⚠️ Sebagian besar selesai | `explorer/ExplorerPanel.kt`: Recent Tabs, Bookmarks, History, Workspace/Trading Sessions, Pinned Websites — semua klik membuka tab. **Downloads** sengaja masih placeholder jujur: JCEF butuh `CefDownloadHandler` yang belum didaftarkan di `JCEFBootstrap`/`JCEFBrowserEngine` (area yang README ini sendiri tandai "paling rawan" untuk disentuh tanpa sepengetahuan kamu). UI-nya sudah siap, tinggal disambungkan kalau kamu OK menyentuh 2 file itu. |
| 4 | Compact Toolbar (-25%) | ✅ Selesai | `theme/Dimens.kt` jadi satu sumber ukuran: Tab bar 36→28dp, Address field custom 32dp (bukan 56dp default M3), padding toolbar 6→4dp. Dipakai konsisten di `BrowserBar.kt`, `AddressBar.kt`, `BrowserTabsBar.kt`. |
| 5 | AI Panel compact | ✅ Diperbaiki sekarang | Sebelumnya konstanta compact (`Dimens.AI_PANEL_*`) sudah didefinisikan tapi **belum pernah dipakai** di `CopilotPanel.kt` (field masih default M3 ~56dp, spacing 16dp, padding 16dp). Sekarang diterapkan: padding panel 16→12dp, spacing antar field pakai `AI_PANEL_VERTICAL_SPACING_DP` (6dp), tombol dipaksa `AI_PANEL_BUTTON_HEIGHT_DP` (34dp), font label/isi dikecilkan. Catatan jujur: `OutlinedTextField` M3 stok tidak bisa dipaksa persis 32dp tanpa `BasicTextField` + `OutlinedTextFieldDefaults.DecorationBox` custom (perubahan lebih besar/berisiko); yang diterapkan sekarang adalah pengecilan real lewat padding+font+tombol, bukan field re-implementasi dari nol. |
| 6 | Browser Menu ala Chrome | ✅ Sebagian besar, sebagian stub jujur | `browser/BrowserMenu.kt`: New Tab, History, Bookmarks, Downloads, Recent Tabs, Find, Zoom, Reset Zoom, Developer Tools, Settings, Exit — jalan sungguhan. New Window / New Incognito Window / Print / Clear Browsing Data — stub (butuh proses/window kedua, `CefRequestContext`, `CefCookieManager`, semuanya level `JCEFBootstrap`/`JCEFBrowserEngine` yang sengaja tidak disentuh tanpa konfirmasi kamu). |
| 7 | Risk Calculator | ✅ Diperbaiki sekarang | Button listener, coroutine, gateway request, JSON parsing sudah jalan (Cloudflare Worker `/api/v1/calculate-risk`). **Sebelumnya** error gateway cuma Text merah inline — **sekarang** pakai `Snackbar` M3 sesuai spec ("Jika Gateway gagal = tampilkan Snackbar"). Berhasil → hasil terisi otomatis (local optimistic preview + hasil gateway). |
| 8 | Analisa Chart | ✅ Diperbaiki sekarang | Screenshot (`java.awt.Robot`) → downscale → compress JPEG → multipart upload (`HttpAIRepository`, Bearer token) → hasil tampil di panel. **Sebelumnya** error cuma Text merah inline — **sekarang** pakai `AlertDialog` modal sesuai spec ("Jika gagal = buat Error Dialog"). |
| 9 | Fullscreen Browser | ✅ Diperbaiki sekarang | `fullscreen/FullscreenController.kt`: semua chrome (toolbar, address bar, activity bar, explorer, AI panel) disembunyikan, muncul lagi otomatis kalau mouse ke atas layar lalu auto-hide lagi. F11 toggle sudah ada; **ESC untuk keluar fullscreen barusan ditambahkan** (sebelumnya cuma F11, ESC belum di-handle sama sekali padahal diminta eksplisit di spec). |
| 10 | Browser UX (shortcut) | ✅ Selesai | `components/KeyboardShortcuts.kt`: Ctrl+T/W/Shift+T/Tab/L/R/F/D, F5, F11, Alt+Left/Right, Middle-click new tab (di tombol +). ESC-exit-fullscreen ditambahkan bersamaan dengan Prioritas 9. |
| 11 | Multi Tab | ✅ Selesai | `browser/BrowserTabsBar.kt`: close, duplicate, pin, mute, reload, drag-reorder, scroll (LazyRow) — via klik kanan/context menu + drag gesture. |
| 12 | Browser Loading | ✅ Selesai | `browser/JCEFBrowserView.kt` + `JCEFBrowserEngine.kt`: loading progress bar (simulasi tahapan, JCEF tidak expose progress byte asli), spinner awal, Error Page, Offline Page, tombol Reload. |
| 13 | UI Polish | ✅ Sebagian besar selesai | Warna/radius/padding/font konsisten lewat `theme/AppColors` + `theme/Dimens` sebagai satu sumber kebenaran, dark theme menyeluruh. Animasi hover/press/fullscreen-reveal pakai `animate*AsState`/`AnimatedVisibility`. |
| 14 | Jangan ubah backend | ✅ Dipatuhi | Tidak ada perubahan di Cloudflare Worker, `HttpAIRepository`/`HttpRiskGatewayRepository` (dipakai apa adanya), `JCEFBootstrap.kt`, atau lifecycle JCEF inti. |
| 15 | README.md | ✅ Bagian ini | — |

### Perbaikan pada sesi ini (di atas hasil sesi sebelumnya)
1. Tooltip ActivityBar yang belum benar-benar tampil (`TooltipBox` di-import tapi tidak dipakai) — diperbaiki.
2. AI Panel (`CopilotPanel.kt`) sekarang benar-benar compact, bukan cuma konstanta yang menganggur di `Dimens.kt`.
3. Snackbar untuk error Risk Calculator (Prioritas 7) & Error Dialog untuk Analisa Chart (Prioritas 8) — sebelumnya cuma teks merah inline.
4. ESC untuk keluar fullscreen (Prioritas 9/10) — sebelumnya belum ada sama sekali.
5. Indentasi `layout/Workbench.kt` yang sempat berantakan (area pemasangan `UpdateBanner`) dirapikan.

### Gap yang sengaja belum disentuh (butuh keputusan kamu)
- **Downloads panel** perlu `CefDownloadHandler` di level JCEF.
- **New Window / Incognito / Print / Clear Browsing Data** di Browser Menu perlu perubahan di `JCEFBootstrap`/`JCEFBrowserEngine`.

Keduanya sengaja tidak diubah karena berada di luar "modul UI Browser" murni dan menyentuh area yang ditandai README asli sebagai paling rawan — beri tahu saya kalau kamu mau saya lanjutkan ke sana juga.
