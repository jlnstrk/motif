import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.google.devtools.ksp.gradle.KspTask
import com.google.devtools.ksp.gradle.KspTaskNative

plugins {
    `multiplatform-conventions`
    `ios-conventions`
    kotlin("native.cocoapods")
    id(libs.plugins.ksp.get().pluginId)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildkonfig)
}

version = "1.0"

kotlin {
    cocoapods {
        summary = "Motif Shared Module"
        homepage = "https://github.com/jlnstrk/motif"
        name = "Shared"
        license = null
        ios.deploymentTarget = "15.0"
        framework {
            baseName = "Shared"
            isStatic = true
            export(project(":player:player-spotify"))
            export(project(":player:player-applemusic"))
            export(libs.multiplatformPaging)
            export(libs.kotlinx.datetime)
            linkerOpts("-lsqlite3")
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.sqldelight.coroutinesExtensions)
                implementation(libs.sqldelight.primitiveAdapters)
                implementation(libs.settings.core)
                implementation(libs.settings.serialization)
                implementation(libs.settings.coroutines)
                api(libs.koin.kotlin)
                api(libs.koin.annotations)
                api(libs.multiplatformPaging)

                implementation(project(":client"))
                api(project(":player"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.koin.test.core)
                implementation(libs.koin.test.junit4)
                implementation(libs.androidx.lifecycle.viewmodel.ktx)
                implementation(libs.sqldelight.driver.android)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.sqldelight.driver.native)
            }
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework> {
            linkerOpts.add("-lsqlite3")
        }
    }
}

buildkonfig {
    packageName = "de.julianostarek.motif.shared"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "SPOTIFY_CLIENT_ID", rootProject.extra["spotify.client.id"] as String)
        buildConfigField(FieldSpec.Type.STRING, "SPOTIFY_CLIENT_SECRET", rootProject.extra["spotify.client.secret"] as String)
        buildConfigField(FieldSpec.Type.STRING, "SPOTIFY_CALLBACK_URI", "motif://auth-callback/spotify")
        buildConfigField(FieldSpec.Type.STRING, "APPLE_DEVELOPER_TOKEN", rootProject.extra["apple.developer.token"] as String)
    }
}

/*tasks.withType<KspTask>().configureEach {
    when (this) {
        is KspTaskNative -> {
            this.compilerPluginOptions.addPluginArgument(
                tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>(compilation.compileKotlinTaskName).get().compilerPluginOptions
            )
        }
    }
}*/

dependencies {
    add("kspCommonMainMetadata", libs.koin.kspCompiler)
    add("kspAndroid", libs.koin.kspCompiler)
    add("kspIosX64", libs.koin.kspCompiler)
    add("kspIosArm64", libs.koin.kspCompiler)
}

sqldelight {
    databases {
        create("MotifDatabase") {
            packageName.set("de.julianostarek.motif.persist")
            dialect(libs.sqldelight.dialect.sqlite.get())
        }
    }
}

android {
    defaultConfig {
        manifestPlaceholders["redirectSchemeName"] = "\${redirectSchemeName}"
        manifestPlaceholders["redirectHostName"] = "\${redirectHostName}"
    }
    namespace = "de.julianostarek.motif.shared"
}