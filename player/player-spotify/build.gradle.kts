
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    alias(libs.plugins.nativeCoroutines)
}

version = "1.0"


kotlin {
    android()
    ios()
    iosArm64()

    explicitApi()

    cocoapods {
        ios.deploymentTarget = "9.0"
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
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.spotify.android.auth)
                api(files("./src/androidMain/libs/spotify-app-remote-release-0.7.2.aar"))
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
    compileSdk = 32
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 28
        targetSdk = 32
    }
}