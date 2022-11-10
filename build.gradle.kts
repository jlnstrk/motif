buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.kotlin.serialization)
        classpath(libs.android.gradlePlugin)
        classpath(libs.google.gradlePlugin)
    }
}

allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx"
                && requested.name == "kotlinx-coroutines-core"
            ) {
                useVersion("1.6.4")
            }
        }
    }
    repositories {
        maven { url = uri("file://${project.rootDir}/repository") }
        google()
        mavenLocal()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

val secretsFile = File(rootDir, "secrets.properties")
if (!secretsFile.exists()) {
    throw GradleException("Missing secrets.properties")
}
val secrets = java.util.Properties()
secrets.load(secretsFile.inputStream())
for ((key, value) in secrets) {
    extra[key.toString()] = value
}