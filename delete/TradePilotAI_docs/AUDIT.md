# Audit Kode — TradePilot AI (Fase 0–9)

Tanggal: 25 Juli 2026
Metode: karena lingkungan kerja tidak memiliki Android SDK, audit dilakukan
dengan kombinasi:

1. **Compile check nyata** memakai Kotlin compiler asli (`kotlinc` v2.0.20,
   diunduh dari GitHub Releases resmi JetBrains) untuk modul `domain`
   (pure Kotlin/JVM, tanpa dependency Android) — hasil: **0 error, 0 warning**.
2. **Analisis statis terprogram** (Python script) yang membaca seluruh 73
   file `.kt` dan 22 `build.gradle.kts`, memvalidasi:
   - Setiap `import com.tradepilot.*` benar-benar mengarah ke deklarasi
     kelas/fungsi yang ada (menangkap typo lintas modul).
   - Setiap parameter `@Inject constructor` punya provider (kelas ber-`@Inject`
     lain, `@Binds`, atau `@Provides`) — validasi wiring Hilt.
   - Keseimbangan kurung `()`, `{}`, `[]` di semua file (menangkap file yang
     terpotong/rusak).
   - Setiap `project(":...")` di `build.gradle.kts` cocok dengan modul yang
     benar-benar terdaftar di `settings.gradle.kts`, dan sebaliknya.
   - Setiap `libs.xxx` yang dipakai di `build.gradle.kts` benar-benar
     terdaftar di `gradle/libs.versions.toml`.
3. **Verifikasi API eksternal** (web search) untuk detail yang sering salah:
   cara autentikasi Gemini API, dan cakupan `material-icons-core` vs
   `material-icons-extended`.

## Hasil & Perbaikan yang Sudah Diterapkan

| # | Temuan | Modul Terdampak | Perbaikan |
|---|---|---|---|
| 1 | `domain` module 100% bersih compile | `domain` | Tidak ada perbaikan diperlukan — dikonfirmasi lewat compile nyata |
| 2 | 0 import internal salah/typo dari 122 deklarasi lintas modul | Semua | Tidak ada perbaikan diperlukan |
| 3 | 0 masalah wiring Hilt (31 parameter `@Inject` tercek, 2 false-positive dari komentar kode) | Semua | Tidak ada perbaikan diperlukan |
| 4 | 0 file dengan kurung tidak seimbang | Semua | Tidak ada perbaikan diperlukan |
| 5 | 0 referensi module Gradle yang salah/hilang | Semua | Tidak ada perbaikan diperlukan |
| 6 | 0 referensi `libs.*` yang tidak terdaftar di version catalog | Semua | Tidak ada perbaikan diperlukan |
| 7 | **Bug nyata**: `ActivityBar.kt` di modul `app` memakai ikon `Analytics`, `BarChart`, `Language`, `MenuBook`, `Shield` yang berada di luar cakupan `material-icons-core` (butuh `material-icons-extended`), tapi modul `app` belum mendeklarasikan dependency itu — akan gagal compile | `app` | Ditambahkan `libs.material.icons.extended` ke `app/build.gradle.kts`; versi dipindah ke `gradle/libs.versions.toml` (sebelumnya hardcode string di `feature-browser`) supaya konsisten satu sumber kebenaran |
| 8 | **Risiko nyata**: 10 modul memakai `kotlinx.coroutines`/`Flow` secara langsung tapi tidak mendeklarasikan `kotlinx-coroutines-core` secara eksplisit — berpotensi gagal compile tergantung apakah dependency transitif (`lifecycle-runtime-ktx`, `room-ktx`) mengekspornya sebagai `api` atau tidak. Terlalu berisiko untuk dibiarkan bergantung pada perilaku transitif library pihak ketiga. | `app`, `core-database`, `data-trading`, `feature-ai`, `feature-analytics`, `feature-journal`, `feature-mentor`, `feature-notification`, `feature-settings`, `feature-trading` | Ditambahkan `implementation(libs.coroutines.core)` eksplisit ke 10 `build.gradle.kts` tersebut |
| 9 | Konfirmasi: header otentikasi Gemini API yang dipakai (`x-goog-api-key`) adalah standar resmi Google saat ini (query param `?key=` masih jalan tapi legacy) | `core-network` | Tidak ada perbaikan diperlukan — sudah benar sejak awal |

## Keterbatasan Audit (Jujur)

Audit ini **BUKAN pengganti compile Gradle+AGP yang sesungguhnya**. Hal-hal
berikut tidak bisa divalidasi tanpa Android SDK:

- Kompatibilitas versi API Jetpack Compose runtime yang presisi (mis. apakah
  parameter lambda `progress = { }` di `LinearProgressIndicator` benar-benar
  ada persis di Compose Material3 versi yang di-resolve BOM `2024.09.03` —
  secara konsep API ini sudah stabil sejak awal 2024 jadi kemungkinan besar
  aman, tapi belum diverifikasi lewat compiler Compose yang sesungguhnya).
- Resource Android (`AndroidManifest.xml`, `strings.xml`, ikon launcher yang
  belum ada) — file `ic_launcher` yang direferensikan di Manifest **belum
  dibuat**, ini AKAN menyebabkan build gagal sampai kamu menambahkan resource
  launcher icon (paling mudah: biarkan Android Studio generate lewat
  `File > New > Image Asset` sekali saja).
- Perilaku runtime (mis. apakah `View.draw(Canvas)` benar-benar menangkap
  konten WebView dengan benar di semua versi Android — ini valid secara API
  tapi ada catatan lama soal hardware-accelerated WebView yang kadang perlu
  `setLayerType(LAYER_TYPE_SOFTWARE)` sebelum capture; ditambahkan sebagai
  catatan TODO, lihat bagian Optimasi Performa di `docs/PERFORMANCE.md`).

## Kesimpulan

Setelah audit ini, seluruh 73 file Kotlin dan 22 modul Gradle di versi 0–7
**konsisten secara struktural dan sintaksis** sejauh yang bisa diverifikasi
tanpa compiler Android penuh. Dua bug nyata (ikon extended, dependency
coroutines eksplisit) sudah diperbaiki. Sisa risiko yang terdokumentasi di
atas (launcher icon belum ada, verifikasi Compose runtime penuh) perlu
diselesaikan saat kamu pertama kali Gradle sync di Android Studio.
