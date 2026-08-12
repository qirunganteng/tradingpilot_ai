pluginManagement {
    val flutterSdkPath =
        run {
            val properties = java.util.Properties()
            file("local.properties").inputStream().use { properties.load(it) }
            val flutterSdkPath = properties.getProperty("flutter.sdk")
            require(flutterSdkPath != null) { "flutter.sdk not set in local.properties" }
            flutterSdkPath
        }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    // Pinned below AGP 9.0: AGP 9.0+ hard-removed getDefaultProguardFile(
    // 'proguard-android.txt'), which flutter_inappwebview_android (and
    // several other Flutter plugins) still reference in their own
    // vendored build.gradle -- see
    // https://github.com/pichillilorenzo/flutter_inappwebview/issues/2852.
    // That file lives in pub-cache, not this repo, so it can't be patched
    // here; pinning AGP is the standard workaround used across the Flutter/
    // Capacitor ecosystem until upstream plugins update. Gradle happily
    // runs an older AGP under a newer Gradle wrapper (the reverse isn't
    // true), so this stays compatible with the bundled Gradle 9.1 wrapper.
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
}

include(":app")
