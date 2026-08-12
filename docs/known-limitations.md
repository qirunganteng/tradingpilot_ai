# Batasan yang diketahui

Berdasarkan `CONSTITUTION.md`, item PRD yang tidak diimplementasikan harus dicatat di sini
secara eksplisit, bukan dilewati begitu saja tanpa keterangan. File ini melacak item
terkait mesin peramban (browser-engine) atau keamanan dari `TradePilot_AI_PRD_SSD.pdf`
yang sengaja tidak (belum) diimplementasikan, alasannya, serta apa yang dibutuhkan
untuk implementasi yang sesungguhnya.

## PRD 2.2.17 "Screen Recorder" (Perekam Layar)

**Status:** tidak diimplementasikan.

**Alasan:** perekaman video layar yang sesungguhnya merupakan masalah yang secara mendasar
berbeda di setiap platform—Windows memerlukan *Desktop Duplication*/*Media Foundation*,
Android memerlukan `MediaProjection`, macOS memerlukan `ScreenCaptureKit`, dan saat ini
tidak ada satu pun plugin Flutter yang membungkus semuanya ke dalam API umum yang
dipelihara secara aktif. Daftar dependensi dalam PRD itu sendiri (`record` +
`ffmpeg_kit_flutter`) sebenarnya juga tidak menyelesaikan masalah ini: `record` menangkap
*audio*, bukan video, sedangkan `ffmpeg_kit_flutter` telah dihentikan dukungannya/ditarik
dari pub.dev pada tahun 2025; paket tersebut perlu di-*fork* dan tetap tidak akan menyediakan
langkah *capture* (penangkapan) tingkat *native* platform yang diperlukan—paket itu hanya
menangani penyandian (*encoding*) setelah *frame* gambar sudah tersedia. Merilis fitur
"perekam layar" yang dibangun di atas paket-paket yang tidak benar-benar merekam layar
akan lebih buruk daripada tidak memiliki fitur tersebut sama sekali: fitur itu akan
menghasilkan *output* kosong atau rusak tanpa ada peringatan.

**Kebutuhan untuk implementasi nyata:** mekanisme *capture* tingkat *native* untuk setiap
platform (`flutter_screen_recording` atau *platform channel* yang dibuat khusus untuk
setiap OS), yang dihubungkan melalui antarmuka `ScreenRecorderManager` di sisi Dart
sehingga bagian aplikasi lainnya (seperti tombol *toolbar* atau indikator perekaman)
tidak perlu mengetahui platform apa yang sedang digunakan. Ini adalah pekerjaan yang
melibatkan kode *native*, bukan sesuatu yang aman dilakukan sebagai tambahan sambil lalu
bersamaan dengan pengembangan mesin peramban lainnya.

## PRD 2.2.18 "Sync" (Sinkronisasi lintas perangkat)

**Status:** tidak diimplementasikan (kait/ *hook* di sisi klien sudah tersedia,
namun tidak ada *backend* yang dapat dipanggil). 
**Alasan:** Fitur sinkronisasi secara eksplisit tercantum dalam Fase 4 PRD ("Fitur Lanjutan", setelah Integrasi AI) pada peta jalan/roadmap (§15.4), 
dan memerlukan rute backend (`POST /api/v1/browser/sync`) yang belum tersedia di `backend/workers/api-gateway`. Membangun *client* sinkronisasi 
untuk endpoint yang belum ada berarti harus melakukan *mocking* pada backend 
(yang memberikan kesan keliru bahwa fitur sudah berfungsi) atau membiarkan *client* dalam kondisi terhubung sebagian dan tidak dapat diuji.

**Kondisi saat ini sebagai gantinya:** penyimpanan lokal untuk setiap jenis data yang dapat disinkronisasi 
(ruang kerja/workspace, tab/sesi, bookmark, riwayat, kata sandi, izin situs, unduhan) 
sudah ditangani oleh manajer khusus (`WorkspaceManager`, `SessionManager`, `HistoryManager`, 
`PasswordVault`, `PermissionManager`, `DownloadManager` — semuanya berada di `lib/features/browser_core/services/`), 
yang masing-masing sudah melakukan serialisasi ke/dari format JSON. Hal ini dilakukan dengan sengaja: 
tujuannya agar integrasi sinkronisasi yang sesungguhnya nanti cukup dengan 
menambahkan panggilan `POST`/`GET` yang membungkus data JSON tersebut pada setiap manajer, bukan merancang format serialisasi dari nol.

## PRD 3.2.4 "Certificate Pinning" -- catatan cakupan

**Status:** sudah diimplementasikan, namun hanya untuk domain backend milik TradePilot sendiri, bukan untuk situs web umum yang dikunjungi pengguna.

Ini bukan merupakan kekurangan, melainkan batasan cakupan yang disengaja dan perlu didokumentasikan: 
lihat komentar pada bagian atas berkas `lib/core/network/certificate_pinning.dart` untuk mengetahui alasannya 
(melakukan *pinning* pada situs web publik akan menyebabkan kegagalan koneksi segera setelah situs tersebut memperbarui/mengganti sertifikatnya, 
sebuah prosedur rutin yang lazim terjadi di web publik). 
Mekanisme ini sudah nyata dan terintegrasi ke dalam *client* Dio bersama (`lib/core/network/api_client.dart`); saat ini mekanisme tersebut tidak aktif 
karena `CertificatePinningConfig.kPinnedPublicKeyHashes` sengaja dikosongkan hingga *hash* kunci publik sertifikat Cloudflare Worker untuk produksi diketahui pada saat *deployment*.

## PRD 3.3.4 "DNS over HTTPS"

**Status:** belum diimplementasikan pada tingkat aplikasi. 
**Alasan:** `flutter_inappwebview` tidak menyediakan fitur untuk mengganti (override) *resolver* DNS—konfigurasi DNS-over-HTTPS (DoH) 
dilakukan pada tingkat sistem operasi atau *stack* jaringan (baik Windows 11 maupun Android mendukung pengaktifan DoH di seluruh sistem). 
Hal ini bukan sesuatu yang dapat diintersepsi secara aman oleh aplikasi Flutter untuk satu *WebView* yang tersemat tanpa menggunakan *stack* jaringan *native* kustom 
(sesuatu yang sengaja dihindari oleh desain `flutter_inappwebview`—lihat catatan dalam `https_enforcer.dart` 
mengenai kebijakan untuk tidak mengimplementasikan ulang lapisan transpor platform). 
Mitigasi praktis yang sudah diterapkan adalah ketentuan PRD 3.2.1/3.2.2 (khusus HTTPS + TLS 1.3), 
yang memastikan *konten* setiap permintaan dienkripsi secara *end-to-end* meskipun pencarian DNS untuk *hostname* tersebut tidak dienkripsi; 
DoH sendiri sebenarnya memberikan manfaat privasi tambahan berupa penyembunyian informasi mengenai *hostname* apa yang sedang dicari dari pengamat jaringan.

**Kebutuhan implementasi nyata:** kode *platform-channel* khusus untuk setiap sistem operasi guna 
(a) mengarahkan *resolver* aplikasi itu sendiri ke *endpoint* DoH (untuk Windows) atau (b) meminta pengguna mengaktifkan pengaturan "Private DNS" bawaan Android, 
karena kedua tindakan tersebut tidak dapat dilakukan hanya melalui Flutter/Dart