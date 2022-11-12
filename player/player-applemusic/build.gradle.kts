plugins {
    `multiplatform-conventions`
    `ios-conventions`
}

kotlin {
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
                api(libs.appleMusic.authentication)
                api(libs.appleMusic.playback)
            }
        }
    }
}