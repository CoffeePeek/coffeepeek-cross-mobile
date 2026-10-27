import com.coffeepeek.config.Config
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION)) }
    }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:network"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
        }
    }
}

android {
    namespace = "${Config.APPLICATION_ID}.legacy.homecheckin"
    compileSdk = Config.COMPILE_SDK
    compileOptions {
        sourceCompatibility = Config.JAVA_VERSION
        targetCompatibility = Config.JAVA_VERSION
    }
}
