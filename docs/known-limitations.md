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

**Status:** backend siap (deployed, `POST`/`GET /api/v1/sync`), tapi baru
**tersinkron per-device** (cadangan cloud, bukan sinkronisasi lintas
perangkat sesungguhnya) -- lihat "Batasan yang disengaja" di bawah. Klien
Flutter (WorkspaceManager, HistoryManager, dkk.) belum dikabelkan untuk
memanggil endpoint ini -- masih tersimpan lokal saja.

**Yang sudah dikerjakan:** tabel `sync_blobs` (device_id, data_type,
payload JSON, updated_at) di-*deploy* ke D1 produksi lewat migration
`0002_sync_blobs.sql`, dan dua endpoint baru:
- `POST /api/v1/sync` -- kirim satu atau lebih blob (`{deviceId, items:
  [{dataType, payload}]}`), payload apa pun bentuknya diteruskan apa
  adanya, backend tidak menafsirkan isinya sama sekali.
- `GET /api/v1/sync?deviceId=...&dataType=...` -- ambil kembali blob yang
  tersimpan untuk perangkat tersebut.

**Batasan yang disengaja:** ini scoped per `deviceId`, bukan per akun
pengguna -- dua perangkat dengan `deviceId` berbeda tidak akan saling
melihat data satu sama lain (karena belum ada sistem akun/login di
backend ini, hanya token gateway tunggal). Jadi hari ini fiturnya adalah
"cadangan cloud yang bisa dipulihkan di perangkat yang sama" (selamat dari
uninstall/reinstall), bukan sinkronisasi lintas perangkat penuh. Lihat
komentar header di `migrations/0002_sync_blobs.sql` untuk alasan lengkap
kenapa dipilih satu tabel blob generik (bukan skema per-kolom PRD §9.2
yang mengasumsikan tabel `users`), dan jalur upgrade ke sinkronisasi
lintas perangkat sesungguhnya (tinggal tambah kolom `user_id` + lapisan
akun/login di atas skema yang sama, tanpa perlu migrasi ulang bentuk
tabelnya).

**Langkah selanjutnya yang tersisa:** (1) kabel `WorkspaceManager`,
`HistoryManager`, `PasswordVault`, `PermissionManager`, `DownloadManager`
di sisi Flutter untuk memanggil endpoint ini (push saat data berubah,
pull saat startup); (2) sistem akun/login (PRD §10.1) supaya `deviceId`
bisa diganti/dilengkapi dengan `user_id` untuk sinkronisasi lintas
perangkat sungguhan.

## PRD 3.2.4 "Certificate Pinning" -- catatan cakupan

**Status:** sudah diimplementasikan, namun hanya untuk domain backend milik TradePilot sendiri, bukan untuk situs web umum yang dikunjungi pengguna.

Ini bukan merupakan kekurangan, melainkan batasan cakupan yang disengaja dan perlu didokumentasikan: 
lihat komentar pada bagian atas berkas `lib/core/network/certificate_pinning.dart` untuk mengetahui alasannya 
(melakukan *pinning* pada situs web publik akan menyebabkan kegagalan koneksi segera setelah situs tersebut memperbarui/mengganti sertifikatnya, 
sebuah prosedur rutin yang lazim terjadi di web publik). 
Mekanisme ini sudah nyata dan terintegrasi ke dalam *client* Dio bersama (`lib/core/network/api_client.dart`); saat ini mekanisme tersebut tidak aktif 
karena `CertificatePinningConfig.kPinnedPublicKeyHashes` sengaja dikosongkan hingga *hash* kunci publik sertifikat Cloudflare Worker untuk produksi diketahui pada saat *deployment*.

## PRD 3.3.2 "Tracking Protection" -- celah di Windows/Linux/web

**Status:** aktif di Android/iOS/macOS. Sengaja dikosongkan (tidak melakukan apa-apa,
bukan diam-diam gagal) di Windows, Linux, dan web.

**Alasan:** `ContentBlocker`/`ContentBlockerActionType` dari `flutter_inappwebview`
adalah pembungkus API `WKContentRuleList` milik WKWebView (iOS/macOS) dan API
intersepsi permintaan milik Android WebView -- tidak ada padanannya di WebView2
(Windows) maupun WebKitGTK (Linux). Ini sempat menyebabkan crash nyata (`type
'Null' is not a subtype of type 'String'`) yang membuat seluruh panel peramban
gagal dirender di Windows: bukan sekadar "fitur ini tidak berfungsi", melainkan
constructor `ContentBlockerActionType.BLOCK` itu sendiri melempar exception di
platform mana pun yang tidak ada dalam tabel nilai per-platform milik enum
tersebut (dikonfirmasi lewat kode sumber paketnya sendiri di
`content_blocker_action_type.g.dart`: hanya ada case untuk android/iOS/macOS,
selebihnya jatuh ke `default: break;` yang mengembalikan `null` untuk kolom
bertipe String non-nullable).

**Perbaikan yang diterapkan:** `buildTrackerContentBlockers()` di
`lib/core/security/tracking_protection_lists.dart` sekarang memeriksa platform
lebih dulu dan mengembalikan daftar kosong di luar Android/iOS/macOS -- objek
`ContentBlocker` tidak pernah dibuat sama sekali di platform yang tidak
didukung, bukan dibuat lalu gagal diam-diam.

**Kebutuhan untuk cakupan penuh di Windows/Linux:** pemblokiran tracker perlu
diimplementasikan lewat jalur lain -- misalnya intersepsi permintaan jaringan
tingkat native (WebView2 punya API `add_WebResourceRequested` untuk ini), yang
berarti kode platform-channel Windows khusus, bukan sesuatu yang tersedia lewat
`flutter_inappwebview` saat ini.

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