
plugins {
    `multiplatform-conventions`
    `ios-conventions`
    kotlin("native.cocoapods")
}

version = "1.0"


kotlin {
    explicitApi()

    cocoapods {
        ios.deploymentTarget = "15.0"
        pod("SpotifyiOS") {
            version = "1.2.2"
            source = path(project.file("./pod"))
        }

        // Suppress warning
        framework {
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit.runtime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.google.code.gson:gson:2.8.9")
                implementation(libs.spotify.android.auth)
                api(libs.spotify.android.appremote)
            }
        }
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.DefFileTask>("generateDefSpotifyiOS").configure {
    doLast {
        outputFile.writeText("""
            language = Objective-C
            headers = $projectDir/pod/SpotifyiOS.framework/Headers/SPTLogin.h $projectDir/pod/SpotifyiOS.framework/Headers/SpotifyAppRemote.h
        """.trimIndent())
    }
}

android {
    defaultConfig {
        manifestPlaceholders["redirectSchemeName"] = "\${redirectSchemeName}"
        manifestPlaceholders["redirectHostName"] = "\${redirectHostName}"
    }
    namespace = "de.julianostarek.motif.player.spotify"
}