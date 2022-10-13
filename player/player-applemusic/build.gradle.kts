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
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
        val commonTest by getting
        val androidMain by getting {
            dependencies {
                implementation(files("./src/androidMain/libs/mediaplayback-release-1.1.1.aar"))
                implementation(files("./src/androidMain/libs/musickitauth-release-1.1.2.aar"))
            }
        }
        val androidTest by getting
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
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