plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// CATATAN: modul ini murni data/network layer (Retrofit, Moshi, provider AI) —
// TIDAK butuh Jetpack Compose sama sekali. Jangan tambahkan buildFeatures.compose
// atau dependency Compose lagi di sini, itu cuma menambah waktu build tanpa manfaat.

android {
    namespace = "com.tradepilot.data.ai"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-security")) // dipakai WorkerProvider & WorkerModule (SecureKeyStore)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}
