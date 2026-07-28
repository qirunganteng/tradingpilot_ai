pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TradePilotAI"

include(":app")

// core
include(":core:core-common")
include(":core:core-ui")
include(":core:core-database")
include(":core:core-network")
include(":core:core-security")
include(":core:core-logging")

// shared (KMP, pure Kotlin, dipakai bersama android-client & desktop-client)
// Sumber kode ada di ../shared (di luar folder android-client) sesuai
// Konstitusi: "Shared Module" adalah proyek terpisah, bukan milik satu Client.
include(":shared")
project(":shared").projectDir = file("../shared")

// data
include(":data:data-user")
include(":data:data-trading")
include(":data:data-ai")

// feature
include(":feature:feature-browser")
include(":feature:feature-ai")
include(":feature:feature-trading")
include(":feature:feature-journal")
include(":feature:feature-notification")
include(":feature:feature-analytics")
include(":feature:feature-drawing")
include(":feature:feature-mentor")
include(":feature:feature-screenshot")
include(":feature:feature-settings")
