# Styles/

Compose tidak punya file "style" terpisah ala Android `styles.xml` -- semua
styling (warna, tipografi, shape) didefinisikan langsung sebagai Kotlin
object/MaterialTheme. Untuk refactor ini:

- Warna & ukuran -> `Theme/Theme.kt`, `Theme/Dimens.kt`
- Animasi -> `Animation/Animations.kt`
- Tipografi -> memakai `MaterialTheme.typography` bawaan (tidak diubah;
  kalau kamu punya font/tipografi custom yang sudah diset di tempat lain di
  monorepo, itu tidak disentuh oleh refactor UI Browser ini).
