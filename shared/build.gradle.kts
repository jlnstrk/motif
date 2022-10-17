import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.google.devtools.ksp.gradle.KspTask
import com.google.devtools.ksp.gradle.KspTaskNative

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.nativeCoroutines)
}

version = "1.0"

kotlin {
    android()
    ios()

    cocoapods {
        summary = "Motif Shared Module"
        homepage = "https://github.com/jlnstrk/motif"
        name = "Shared"
        license = null
        ios.deploymentTarget = "13.0"
        framework {
            baseName = "Shared"
            isStatic = true
            export(project(":player:player-spotify"))
            export(project(":player:player-applemusic"))

            linkerOpts("-lsqlite3")
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                api(libs.koin.kotlin)
                api(libs.koin.annotations)
                implementation(libs.sqldelight.coroutinesExtensions)
                implementation(libs.sqldelight.primitiveAdapters)
                implementation(libs.settings.core)
                implementation(libs.settings.serialization)
                implementation(libs.settings.coroutines)
                implementation(project(":backend"))
                api(project(":player:player-spotify"))
                api(project(":player:player-applemusic"))
            }
        }
        val commonTest by getting
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.lifecycle.viewmodel.ktx)
                implementation(libs.sqldelight.driver.android)
            }
        }
        val androidTest by getting
        val iosMain by getting {
            dependencies {
                implementation(libs.sqldelight.driver.native)
            }
        }
        val iosTest by getting
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework> {
            linkerOpts.add("-lsqlite3")
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    }
}

buildkonfig {
    packageName = "de.julianostarek.motif.shared"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "SPOTIFY_CLIENT_ID", rootProject.extra["spotify.client.id"] as String)
        buildConfigField(FieldSpec.Type.STRING, "SPOTIFY_CALLBACK_URI", "motif://spotify-auth-callback")
        buildConfigField(FieldSpec.Type.STRING, "APPLE_DEVELOPER_TOKEN", rootProject.extra["apple.developer.token"] as String)
    }
}

tasks.withType<KspTask>().configureEach {
    when (this) {
        is KspTaskNative -> {
            this.compilerPluginOptions.addPluginArgument(
                tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>(compilation.compileKotlinTaskName).get().compilerPluginOptions
            )
        }
    }
}

nativeCoroutines {
    suffix = "Native"
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.kspCompiler)
    add("kspAndroid", libs.koin.kspCompiler)
    add("kspIosX64", libs.koin.kspCompiler)
    add("kspIosArm64", libs.koin.kspCompiler)
}

sqldelight {
    database("MotifDatabase") {
        packageName = "de.julianostarek.motif.persist"
        dialect(libs.sqldelight.dialect.sqlite.get())
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