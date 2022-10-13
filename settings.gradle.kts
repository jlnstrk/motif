enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "motif"
include(":android")
include(":shared")
include(":backend")
include(":player")
include(":player:player-spotify")
include(":player:player-applemusic")