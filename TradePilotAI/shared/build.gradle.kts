plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

// KONSTITUSI: shared HARUS pure Kotlin, tidak boleh depend ke Android SDK
// atau library spesifik-platform apa pun. Semua Business Logic
// (Risk Engine, Use Case, Repository interface, Model, dst) hidup di sini
// dan dipakai bersama oleh android-client & desktop-client.

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation("javax.inject:javax.inject:1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting
        val desktopMain by getting
    }
}

android {
    namespace = "com.tradepilot.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultMinSdk(libs.versions.minSdk.get().toInt())
}

// Helper kecil biar baris `defaultMinSdk` di atas jelas maksudnya dan
// tidak salah ketik dengan `minSdk` milik com.android.application.
fun com.android.build.gradle.LibraryExtension.defaultMinSdk(value: Int) {
    this.defaultConfig.minSdk = value
}
