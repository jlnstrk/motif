plugins {
    id("com.android.application")
    kotlin("android")
    // id("com.google.gms.google-services")
}

repositories {
    maven { url = uri("https://androidx.dev/storage/compose-compiler/repository/") }
}

android {
    sourceSets.getByName("main").java.srcDir("src/main/kotlin")
    compileSdk = 33
    
    defaultConfig {
        applicationId = "de.julianostarek.motif"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["redirectSchemeName"] = "motif"
        manifestPlaceholders["redirectHostName"] = "auth-callback"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlinOptions {
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
        )
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":client"))
    implementation(project(":player:player-spotify"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.accompanist.drawablePainter)
    implementation(libs.accompanist.placeholder.material)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.palette)

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.coil.kt.compose)

    implementation(libs.androidx.window)

    implementation(libs.accompanist.pager)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    coreLibraryDesugaring(libs.core.jdk.desugaring)

    implementation(project(":player:player-applemusic:musickit-auth"))
    implementation(project(":player:player-applemusic:musickit-mediaplayback"))
    implementation(project(":player:player-spotify:spotify-app-remote"))
}