import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Signing config release: SEMUA nilai sensitif (path keystore, password)
// dibaca dari local.properties (TIDAK PERNAH di-commit ke git — sudah ada
// di .gitignore) atau dari environment variable (dipakai di GitHub Actions
// lewat GitHub Secrets). Kalau tidak diisi, build release akan gagal
// dengan pesan jelas alih-alih diam-diam pakai signing debug.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}
fun signingProp(key: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(key)

android {
    namespace = "com.tradepilot.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.tradepilot.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "0.2.0-beta" // Fase 0-9 + Cloudflare Worker Gateway selesai
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            isMinifyEnabled = false
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProp("RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingProp("RELEASE_STORE_PASSWORD")
                keyAlias = signingProp("RELEASE_KEY_ALIAS")
                keyPassword = signingProp("RELEASE_KEY_PASSWORD")
            }
            // Kalau storeFilePath null (belum ada keystore.properties/env var),
            // signingConfig ini sengaja dibiarkan tidak lengkap — build release
            // akan gagal dengan pesan error Gradle standar yang jelas, alih-alih
            // diam-diam fallback ke signing debug (bahaya untuk rilis publik).
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":data:data-user"))
    implementation(project(":data:data-trading"))
    implementation(project(":data:data-ai"))
    // core
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-security"))
    implementation(project(":core:core-network")) // supaya bisa daftarkan coreNetworkModule di startKoin
    implementation(project(":core:core-logging"))

    // domain
    implementation(project(":shared"))

    // feature (app hanya bergantung ke feature, bukan sebaliknya)
    implementation(project(":feature:feature-browser"))
    implementation(project(":feature:feature-ai"))
    implementation(project(":feature:feature-trading"))
    implementation(project(":feature:feature-journal"))
    implementation(project(":feature:feature-notification"))
    implementation(project(":feature:feature-analytics"))
    implementation(project(":feature:feature-drawing"))
    implementation(project(":feature:feature-mentor"))
    implementation(project(":feature:feature-screenshot"))
    implementation(project(":feature:feature-settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.material.icons.extended) // ActivityBar: Analytics/BarChart/Language/MenuBook/Shield
    implementation(libs.navigation.compose)

    // dipakai AppChartSnapshotProvider/AppRootViewModel untuk capture WebView
    implementation(project(":core:core-database"))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coroutines.core)
}
