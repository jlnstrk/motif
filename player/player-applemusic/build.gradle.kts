plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.nativeCoroutines)
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
                implementation(libs.kotlinx.datetime)
            }
        }
        val androidMain by getting {
            dependencies {
                compileOnly(project(":player:player-applemusic:musickit-auth"))
                compileOnly(project(":player:player-applemusic:musickit-mediaplayback"))
            }
        }
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