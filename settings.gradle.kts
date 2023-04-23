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
include(":client")
include(":player")
include(":player:player-spotify")
include(":player:player-applemusic")

include(":player:player-applemusic:musickit-auth")
include(":player:player-applemusic:musickit-mediaplayback")
include(":player:player-spotify:spotify-app-remote")