# AIWorkspace/ -- TIDAK ADA FILE DI SINI

Prioritas 5 (AI Panel) minta Balance/Risk/Entry/Stop Loss/Take Profit/Text
Field/Button/vertical spacing diperkecil ala VSCode Side Panel.

File yang berisi UI itu (kemungkinan besar bernama sesuatu seperti
`CopilotPanel.kt` di `desktop-client`) **TIDAK ada di paket
`TradePilotAI-BrowserFiles.zip`** yang kamu upload -- README asli di paket
itu memang bilang scope-nya cuma modul UI Browser murni.

## Yang sudah saya siapkan
`Theme/Dimens.kt` sudah punya konstanta target buat panel ini:

```kotlin
AI_PANEL_FIELD_HEIGHT_DP = 32     // dari default M3 ~56dp
AI_PANEL_VERTICAL_SPACING_DP = 6  // dari biasanya 12-16dp
AI_PANEL_BUTTON_HEIGHT_DP = 34
```

## Cara terapkan ke file aslinya
1. Cari file yang render Balance/Risk/Entry/SL/TP (kemungkinan
   `CopilotPanel.kt` atau serupa).
2. Ganti tiap `OutlinedTextField` tanpa height override jadi punya
   `.height(Dimens.AI_PANEL_FIELD_HEIGHT_DP.dp)` (atau ganti ke
   `BasicTextField` custom kalau OutlinedTextField M3 menolak dipaksa kecil
   -- lihat contoh nyata di `AddressBar/AddressBar.kt` yang mengalami
   masalah sama).
3. Ganti `Spacer(Modifier.height(12.dp))` / `Arrangement.spacedBy(16.dp)`
   antar field jadi `Dimens.AI_PANEL_VERTICAL_SPACING_DP.dp`.
4. Import warna dari `Theme/Theme.kt` (`AppColors`) supaya konsisten dengan
   panel lain, bukan hardcode `Color(0xFF...)` lokal.

Kirimkan isi file `CopilotPanel.kt` (atau nama file aslinya) kalau mau saya
kerjakan langsung persis seperti yang lain, bukan cuma dipandu.
