plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    alias(libs.plugins.apollo)
    alias(libs.plugins.ksp)
}

version = "1.0"

kotlin {
    android()
    ios()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.kotlin)
                implementation(libs.koin.annotations)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
                api(libs.apollo.runtime)
                api(libs.apollo.normalizedCache.sqlite)
            }
        }
        val commonTest by getting
        val androidMain by getting
        val androidTest by getting
    }
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.koin.kspCompiler)
    add("kspAndroid", libs.koin.kspCompiler)
    add("kspIosX64", libs.koin.kspCompiler)
    add("kspIosArm64", libs.koin.kspCompiler)
}

/*
Update schema:
./gradlew client:downloadApolloSchema --endpoint='https://motif.julianostarek.de/graphql' --schema=client/src/commonMain/graphql/de/julianostarek/motif/client/schema.graphqls
./gradlew client:generateApolloSources
 */

apollo {
    packageName.set("de.julianostarek.motif.client")
    mapScalar("DateTime", "kotlinx.datetime.Instant")
    mapScalarToKotlinString("UUID")
}