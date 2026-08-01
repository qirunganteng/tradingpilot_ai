# RiskCalculator/ -- TIDAK ADA FILE DI SINI

Prioritas 7 minta perbaikan tombol "Hitung Risk": Button Listener, Coroutine,
Gateway Request, Authentication, Response Parsing, Timeout, JSON Parsing,
Logging, Snackbar kalau gagal, auto-fill kalau berhasil.

**Ini bukan bug UI** -- ini logic (network call ke AI Gateway, auth, parsing
response). File yang berisi logic itu (button handler + panggilan ke
Gateway) tidak ada di `TradePilotAI-BrowserFiles.zip`, dan sesuai instruksi
kamu sendiri di Prioritas 14 ("JANGAN mengubah Backend/AI Gateway/API"), ini
memang di luar scope modul UI Browser yang saya kerjakan.

## Yang saya butuhkan buat menindaklanjuti ini
Kirimkan file yang berisi:
1. Composable/Activity tombol "Hitung Risk" beserta `onClick` handler-nya.
2. Class/fungsi yang memanggil AI Gateway (mis. Retrofit service, Ktor
   client, atau fungsi `suspend fun calculateRisk(...)`).
3. (Kalau ada) log error terakhir saat tombol ditekan dan tidak terjadi
   apa-apa -- ini paling cepat buat nebak apakah masalahnya di listener yang
   tidak terpasang, coroutine yang di-launch tapi exception-nya ketelan
   (silent catch), atau response yang gagal di-parse.

Begitu ada filenya, saya bisa telusuri satu-satu (listener -> coroutine
scope -> request -> auth header -> response parsing -> UI update) seperti
yang diminta di Prioritas 7, dan kasih Snackbar/error handling yang jujur
(bukan asal nampilin "berhasil" walau gagal).
