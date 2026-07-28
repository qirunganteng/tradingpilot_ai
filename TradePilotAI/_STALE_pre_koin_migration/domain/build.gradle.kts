plugins {
    alias(libs.plugins.kotlin.jvm)
}

// PENTING: domain HARUS pure Kotlin — tidak boleh depend ke Android SDK
// atau library Android manapun, supaya bisa di-unit-test tanpa emulator
// dan berpotensi di-share ke Kotlin Multiplatform di masa depan.

dependencies {
    implementation(libs.coroutines.core)
    implementation("javax.inject:javax.inject:1")
    testImplementation("junit:junit:4.13.2")
}
