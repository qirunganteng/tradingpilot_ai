# Panduan Deploy — Cloudflare → GitHub → GitHub Actions

Panduan ini urut langkah demi langkah dari nol sampai app+backend jalan dan
ada CI/CD otomatis. Ikuti berurutan — setiap bagian butuh hasil dari bagian
sebelumnya.

---

## Bagian A — Deploy Cloudflare Worker (AI Gateway) + D1 + R2

### A.1 Prasyarat
- Akun Cloudflare (gratis cukup untuk mulai — Workers, D1, R2 semua punya free tier).
- Node.js 18+ terinstal di komputer kamu.
- API key Gemini kamu (yang sudah kamu punya).

### A.2 Install Wrangler & Login

```bash
cd cloudflare-worker
npm install
npx wrangler login
```

Ini akan buka browser untuk login/otorisasi Wrangler ke akun Cloudflare kamu.

### A.3 Buat Database D1

```bash
npx wrangler d1 create tradepilot_db
```

Output-nya akan berisi blok seperti ini — **salin `database_id`-nya**:
```toml
[[d1_databases]]
binding = "DB"
database_name = "tradepilot_db"
database_id = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
```

Buka `wrangler.toml`, ganti `REPLACE_WITH_YOUR_D1_DATABASE_ID` dengan ID asli itu.

### A.4 Jalankan Migration D1

```bash
npm run db:migrate:remote
```

Ini menjalankan `migrations/0001_init.sql` (bikin tabel `analyses` dan `request_log`) di database D1 asli di Cloudflare.

### A.5 Buat Bucket R2

```bash
npx wrangler r2 bucket create tradepilot-charts
```

Nama bucket ini harus **persis sama** dengan `bucket_name` di `wrangler.toml` (`tradepilot-charts`) — kalau kamu ganti nama, update juga di `wrangler.toml`.

### A.6 Buat KV Namespace (untuk rate limit)

```bash
npx wrangler kv namespace create RATE_LIMIT_KV
```

Salin `id` yang muncul, ganti `REPLACE_WITH_YOUR_KV_NAMESPACE_ID` di `wrangler.toml`.

### A.7 Isi Secret (API Key & Token)

**Jangan pernah taruh nilai ini di `wrangler.toml`** — secret disimpan terenkripsi di Cloudflare, terpisah dari kode:

```bash
npx wrangler secret put GEMINI_API_KEY
# paste API key Gemini kamu saat diminta, lalu Enter

npx wrangler secret put GATEWAY_AUTH_TOKEN
# buat token acak yang panjang, mis. generate dengan:
#   openssl rand -hex 32
# paste hasilnya saat diminta
```

**Catat `GATEWAY_AUTH_TOKEN` ini di tempat aman** — nanti kamu isi nilai yang SAMA di app Android (Settings → Gateway Token) dan di GitHub Secrets (Bagian C).

### A.8 Deploy

```bash
npm run deploy
```

Output akan menunjukkan URL Worker kamu, bentuknya:
```
https://tradepilot-ai-gateway.<subdomain-kamu>.workers.dev
```

**Catat URL ini** — nanti diisi di app Android (Settings → URL Worker).

### A.9 Verifikasi Deploy Berhasil (dari browser PC atau HP)

Buka di browser (PC atau HP, sama saja — ini cuma REST API):
```
https://tradepilot-ai-gateway.<subdomain-kamu>.workers.dev/api/v1/health
```
Harus muncul JSON: `{"status":"ok","environment":"production","time":...}`.

Kalau ini sudah muncul, Worker + D1 + R2 sudah live dan siap dipakai app.

### A.10 (Opsional) Test Endpoint Analyze Manual dari PC

```bash
curl -X POST "https://tradepilot-ai-gateway.<subdomain-kamu>.workers.dev/api/v1/analyze" \
  -H "x-gateway-token: TOKEN_YANG_KAMU_BUAT_DI_A.7" \
  -H "Content-Type: application/json" \
  -d '{"imageBase64":"<base64 gambar kecil>","deviceId":"test-pc"}'
```

---

## Bagian B — Upload ke GitHub

### B.1 Buat Repository Baru di GitHub

Lewat browser: github.com → New repository → beri nama (mis. `tradepilot-ai`) → **jangan** centang "Add README" (karena project sudah ada) → Create repository.

### B.2 Inisialisasi Git Lokal & Push

Dari folder root project (yang berisi folder `TradePilotAI/` dan `cloudflare-worker/` sejajar — atau gabungkan keduanya dulu jadi satu folder root sesuai struktur kamu):

```bash
cd /path/ke/folder/project
git init
git add .
git commit -m "Initial commit: TradePilot AI (Fase 0-9) + Cloudflare Worker Gateway"
git branch -M main
git remote add origin https://github.com/USERNAME/tradepilot-ai.git
git push -u origin main
```

Ganti `USERNAME` dan nama repo sesuai punya kamu. Kalau diminta login, pakai **Personal Access Token** (bukan password akun) — buat di GitHub → Settings → Developer settings → Personal access tokens.

### B.3 Pastikan File Sensitif TIDAK Ter-push

**Sebelum** `git push`, jalankan ini untuk memastikan tidak ada rahasia ter-commit:

```bash
git status
# pastikan keystore.properties, *.jks, node_modules/, .env TIDAK muncul di daftar
```

`.gitignore` sudah disiapkan untuk mengecualikan file-file ini — kalau ada yang tetap muncul, jangan push dulu, cek `.gitignore`.

---

## Bagian C — GitHub Actions (CI/CD Otomatis)

Ada 2 workflow yang sudah disiapkan di `.github/workflows/`:
1. `android-build.yml` — build & test APK setiap push (sudah ada dari Fase 9).
2. `worker-deploy.yml` — deploy Cloudflare Worker otomatis setiap push ke `main` (baru, lihat di bawah).

### C.1 Isi GitHub Secrets

Repo GitHub kamu → **Settings → Secrets and variables → Actions → New repository secret**. Tambahkan:

| Nama Secret | Isi |
|---|---|
| `CLOUDFLARE_API_TOKEN` | Buat di Cloudflare dashboard → My Profile → API Tokens → Create Token → template "Edit Cloudflare Workers" |
| `CLOUDFLARE_ACCOUNT_ID` | Ada di Cloudflare dashboard, sidebar kanan halaman Workers |
| `RELEASE_STORE_FILE_BASE64` | Hasil `base64 -i tradepilot-release.jks` (keystore Android untuk signing release) |
| `RELEASE_STORE_PASSWORD` | Password keystore kamu |
| `RELEASE_KEY_ALIAS` | Alias keystore kamu |
| `RELEASE_KEY_PASSWORD` | Password key kamu |

### C.2 Cara Kerja Workflow Worker Deploy

Setiap kali kamu `git push` ke branch `main` dengan perubahan di folder `cloudflare-worker/`, GitHub Actions otomatis:
1. Install dependency.
2. Jalankan `npm test` (Vitest) — **kalau gagal, deploy dibatalkan otomatis**.
3. Jalankan `npm run typecheck`.
4. Deploy ke Cloudflare pakai `CLOUDFLARE_API_TOKEN`.

### C.3 Cara Kerja Workflow Android Build

Setiap push, GitHub Actions:
1. Build `debug` APK (selalu, cepat, untuk verifikasi cepat tiap commit).
2. Kalau push ke `main` dan semua secret signing terisi → build `release` APK bertanda tangan, upload sebagai artifact yang bisa kamu download dari tab **Actions** di GitHub.

### C.4 Memicu & Memantau

```bash
git add .
git commit -m "Update fitur X"
git push
```

Lalu buka tab **Actions** di repo GitHub kamu (bisa dibuka dari browser PC atau HP — GitHub web sepenuhnya responsive) untuk lihat progress build & deploy secara real-time.

---

## Bagian D — Setelah Semua Live: Hubungkan App ke Worker

1. Install APK (debug dulu untuk testing) ke HP Android.
2. Buka app → Settings.
3. Isi **URL Worker** dari hasil Bagian A.8.
4. Isi **Gateway Token** — nilai SAMA PERSIS dengan yang kamu buat di Bagian A.7.
5. Kembali ke Browser, login Exness, tekan ANALISA.
6. Kalau berhasil, cek dashboard Cloudflare (D1 → Browse tabel `analyses`, R2 → bucket `tradepilot-charts`) — data baru harus muncul di sana.

Selesai — App ↔ Worker ↔ Gemini ↔ D1/R2 sudah terhubung penuh, dan setiap
push ke GitHub otomatis mem-build & (untuk Worker) mem-deploy ulang.
