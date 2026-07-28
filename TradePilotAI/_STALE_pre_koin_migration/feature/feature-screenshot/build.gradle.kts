plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// CATATAN: modul ini murni capture/compress Bitmap (android.graphics),
// TIDAK butuh Jetpack Compose. Jangan tambahkan buildFeatures.compose lagi.

android {
    namespace = "com.tradepilot.feature.screenshot"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}
