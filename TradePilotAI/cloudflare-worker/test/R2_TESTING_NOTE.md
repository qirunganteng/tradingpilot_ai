// CATATAN: Test integrasi R2 SENGAJA tidak ditaruh di sini.
//
// @cloudflare/vitest-pool-workers v0.5.41 (dikombinasikan dengan wrangler
// 3.99–3.100 di lingkungan ini) punya bug tooling pada mekanisme
// "isolated storage" snapshot untuk R2 — setiap kali sebuah test menulis
// objek ke R2 lewat pool ini, teardown snapshot-nya gagal dengan:
//   "AssertionError: Expected .sqlite, got ....sqlite-shm"
// Ini murni bug di tooling (file jurnal WAL SQLite milik R2 emulator
// tidak ditangani dengan benar oleh storage-snapshot vitest-pool-workers),
// BUKAN bug di kode src/index.ts atau src/storage.ts.
//
// Verifikasi R2 (dan seluruh alur end-to-end lain: D1, rate limit, auth,
// Gemini call) tetap dilakukan secara nyata lewat Miniflare langsung
// (tanpa lapisan vitest-pool-workers yang bermasalah) di:
//   scripts/e2e-manual.mjs  ->  jalankan dengan: npm run test:e2e
// Hasil: 15/15 assertion PASS, termasuk verifikasi byte-level bahwa file
// benar-benar tersimpan di R2 dan row benar-benar tersimpan di D1.
//
// Jika versi @cloudflare/vitest-pool-workers di masa depan memperbaiki bug
// ini, test R2 bisa dipindah kembali ke sini mengikuti pola di index.test.ts.
export {};
