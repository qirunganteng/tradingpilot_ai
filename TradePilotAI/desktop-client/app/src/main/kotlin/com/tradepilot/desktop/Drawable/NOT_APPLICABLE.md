# Drawable/

Folder ini kosong secara sengaja.

`desktop-client` adalah aplikasi **Compose for Desktop (JVM)**, bukan Android.
Konsep "drawable XML" (vector drawable, nine-patch, dsb.) itu spesifik ke
Android resource system dan tidak berlaku di sini.

Semua icon di refactor ini pakai Material Icons Extended langsung dari kode
Kotlin (lihat `Icons/IconMapping.kt`). Kalau ke depan butuh gambar custom
(logo, ilustrasi), itu ditaruh sebagai file `.svg`/`.png` biasa di folder
resource Compose Desktop (`src/main/resources/`) dan dipanggil lewat
`painterResource("nama_file.svg")` -- BUKAN lewat sistem drawable Android.
