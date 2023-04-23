plugins {
    `multiplatform-conventions`
    kotlin("plugin.serialization")
}

kotlin {
    android()
    ios()

    explicitApi()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.json)
                implementation(libs.spotify.mp.core)
                api(project(":player:player-spotify"))
                api(project(":player:player-applemusic"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

android {
    defaultConfig {
        manifestPlaceholders["redirectSchemeName"] = "\${redirectSchemeName}"
        manifestPlaceholders["redirectHostName"] = "\${redirectHostName}"
    }
    namespace = "de.julianostarek.motif.player"
}