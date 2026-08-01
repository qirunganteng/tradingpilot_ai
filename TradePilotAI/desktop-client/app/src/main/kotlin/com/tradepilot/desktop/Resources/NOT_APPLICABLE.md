# Resources/

Sama seperti `Drawable/` -- folder ini kosong secara sengaja untuk
`desktop-client`. Tidak ada `strings.xml`, `colors.xml`, `dimens.xml` ala
Android di project Compose Desktop; padanannya sudah ada di:

- `Theme/Theme.kt`   -> warna (padanan `colors.xml`)
- `Theme/Dimens.kt`  -> ukuran (padanan `dimens.xml`)

Kalau butuh string yang bisa diterjemahkan (i18n), itu pattern terpisah di
Compose Multiplatform (`stringResource` + `.xml`/`.properties` di
`src/commonMain/composeResources/`) -- di luar cakupan refactor UI Browser
kali ini karena tidak ada teks yang perlu di-i18n-kan di modul browser.
