// Root build.gradle.kts — hanya deklarasi plugin versi, tidak apply di root.
//
// PENTING: pakai alias(libs.plugins.xxx) di sini, BUKAN id("...") version "..." —
// mencampur dua gaya deklarasi untuk plugin yang sama (id+version literal di root
// vs alias version-catalog di submodule) memicu bug Gradle "plugin is already on
// the classpath with an unknown version" (lihat gradle/gradle#20084). Dengan
// alias(...) di root DAN di submodule, keduanya resolve lewat satu jalur yang sama
// (gradle/libs.versions.toml), tidak ada ambiguitas versi.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false // <-- TAMBAHKAN INI
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
