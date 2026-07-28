# backend/cloudflare-worker

Backend AI Gateway (Gemini proxy) + D1 + R2. Isi tidak diubah dari yang
kamu upload — hanya `node_modules/` dan `.wrangler/` (cache lokal) yang
dihapus karena itu hasil generate, bukan source code. Jalankan lagi:

```
cd backend/cloudflare-worker
npm install
npm test        # vitest — 14+15 test yang disebut di docs/TESTING.md
npx wrangler dev
```

Deploy tetap ikuti `docs/DEPLOY.md` (kalau file itu ada di repo lama kamu,
belum ikut ter-upload ke saya — kirim juga kalau perlu saya cek).
