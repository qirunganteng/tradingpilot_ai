import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    application
}

// KONSTITUSI: module ini (Platform Client) TIDAK BOLEH berisi Business
// Logic. Hanya UI rendering, window management, dan platform integration.
// Semua logic (Risk Engine, Use Case, dst) ada di :shared.

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(libs.coroutines.core)

    // Fase 5: Browser Engine (JCEF) -- lihat browser/JCEFBootstrap.kt untuk
    // catatan penting soal native binary & first-run download.
    implementation(libs.jcefmaven)
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("com.tradepilot.desktop.MainKt")
}

compose.desktop {
    application {
        mainClass = "com.tradepilot.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TradePilot AI"
            // MAJOR HARUS > 0 -- format Dmg (macOS) mewajibkan ini walau kita
            // build Windows, karena Compose Gradle plugin memvalidasi versi
            // untuk SEMUA targetFormats yang terdaftar di atas, bukan cuma
            // yang benar-benar di-build. "0.1.0" (MAJOR=0) gagal validasi
            // itu meski formatnya "terlihat" benar (lihat error CI: "Illegal
            // version for 'Dmg': '0.1.0' is not a valid version").
            packageVersion = "1.0.0"
            description = "TradePilot AI — Trading Workspace (Desktop)"
        }
    }
}
