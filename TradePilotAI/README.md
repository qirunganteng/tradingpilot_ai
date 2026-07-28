# TradePilot AI Platform — Monorepo

Struktur ini mengikuti `CONSTITUTION.md` (Single Platform, Multiple Clients,
Shared Business Logic, Backend First, Client Thin).

```
tradepilot-platform/
├── shared/           # KMP, pure Kotlin — Business Logic dipakai bersama
├── android-client/   # App Android (project TradePilotAI yang sudah ada)
├── desktop-client/   # App Desktop (Compose Multiplatform + JCEF, baru)
├── backend/          # Cloudflare Worker (belum ter-upload, lihat backend/README.md)
├── docs/             # Blueprint, AUDIT, TESTING, PERFORMANCE, RELEASE_CHECKLIST
└── tools/            # (kosong, untuk script lintas-platform ke depan)
```

## Status — apa yang sudah dikerjakan (Fase 0, 1 & 2)

**Fase 0 — Restrukturisasi repo (selesai)**
- Repo dipecah jadi `shared/ backend/ android-client/ desktop-client/ docs/ tools/`.
- `android-client/` = project Android yang sekarang (app, core, data, feature),
  isinya tidak diubah kecuali `domain/` yang dipindah keluar.
- `docs/` (Blueprint_v0.md, AUDIT.md, TESTING.md, PERFORMANCE.md,
  RELEASE_CHECKLIST.md) dipindah ke level platform.
- `backend/cloudflare-worker/` sudah dipindah masuk (tanpa `node_modules`/`.wrangler`).

**Fase 1 — Shared module KMP (selesai, untuk bagian domain)**
- `domain/` (yang memang sudah pure Kotlin, tanpa Android SDK) dipindah jadi
  `shared/src/commonMain/kotlin/...` — isi file/package **tidak diubah sama sekali**,
  cuma lokasinya.
- `shared/build.gradle.kts` baru: target `androidTarget()` + `jvm("desktop")`.

**Fase 2 — Hilt → Koin (selesai secara tekstual, BELUM di-build)**
- Semua 19 `build.gradle.kts` (core-*, data-*, feature-*, app) bersih dari
  Hilt/ksp-hilt, sudah pakai Koin (`io.insert-koin:koin-android` 4.0.0 +
  `koin-androidx-compose` untuk module yang punya ViewModel Compose).
- Semua `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`,
  `@Module/@InstallIn/@Provides/@Binds`, `@Inject`, `@Singleton`,
  `javax.inject.*` sudah dihapus di seluruh `android-client` (sudah di-grep
  ulang berkali-kali, bersih — sisa cuma komentar historis yang menyebut
  nama Hilt/Dagger, tidak masalah).
- 20 Koin module baru: `coreCommonModule`, `coreSecurityModule`,
  `coreNetworkModule`, `coreDatabaseModule`, `dataUserModule`,
  `dataTradingModule`, `dataAiGeminiModule` + `dataAiWorkerModule` +
  `dataAiProviderModule`, `useCaseModule` (baru — daftar semua use case
  dari `:shared`, lihat komentar di file itu kenapa dipusatkan di `:app`),
  10 module `feature*Module` (masing-masing daftar `viewModel { }`), dan
  `appModule`. Semua didaftarkan di `TradePilotApplication.kt` lewat
  `startKoin { modules(...) }`.
- Qualifier custom Dagger (`@ApiKeyInterceptorQualifier` dkk, `@WorkerRetrofit`)
  diganti `named(...)` — lihat `NetworkQualifiers.kt` dan `WorkerModule.kt`.
- `hiltViewModel()` → `koinViewModel()` di 10 Screen + `TradePilotNavHost.kt`.

⚠️ **PENTING soal `TradePilotApplication.kt`**: file aslinya sempat rusak
(CRLF + terpotong saat saya baca ulang di sesi sebelumnya), jadi bagian
`installCrashHandler()` saya rekonstruksi ulang berdasarkan `CrashActivity.kt`
yang ada. Saya sudah cocokkan method-nya (`CrashActivity.start(...)`, bukan
`.launch(...)` seperti tebakan awal saya), tapi **tolong cek ulang file ini**
kalau logic crash-handling aslinya lebih rumit dari yang saya rekonstruksi.

**Desktop client — skeleton awal**
- `desktop-client/` project Gradle terpisah (Compose Multiplatform Desktop),
  `Main.kt` sudah manggil `CalculateRiskUseCase` dari `:shared` sebagai bukti
  wiring shared-module nyambung.
- Browser Engine (JCEF) **belum** dipasang.

## Belum dikerjakan / butuh keputusan kamu

1. **Belum pernah di-build.** Sandbox saya tidak punya akses ke Google's
   Maven (`dl.google.com`) atau JetBrains Compose repo, jadi saya tidak bisa
   menjalankan `./gradlew build` untuk memverifikasi ini kompilasi bersih.
   **Ini langkah paling penting yang harus kamu lakukan duluan** sebelum
   lanjut ke fase berikutnya — kemungkinan besar ada 1-2 typo/qualifier
   yang perlu disesuaikan begitu Gradle sync jalan.
2. **Fase 5 (JCEF browser engine di desktop) belum diimplementasi** — masih
   placeholder teks di `Workspace()`.
3. Root `tradepilot-platform/` sengaja **tidak** dibuat jadi satu Gradle
   root tunggal (android-client & desktop-client masing-masing punya
   `settings.gradle.kts` sendiri, sama-sama include `:shared` lewat
   `projectDir` relatif).
