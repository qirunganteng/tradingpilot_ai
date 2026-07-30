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
