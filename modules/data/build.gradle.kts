import com.coffeepeek.buildlogic.module
import com.coffeepeek.config.Config
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION))
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
            implementation("androidx.security:security-crypto:1.0.0")
        }
        commonMain.dependencies {
            implementation(project(module.legacy.domain))
            implementation(project(module.legacy.network))
            implementation(project(module.legacy.room))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
            implementation(libs.ktor.client.content.negotiation)
        }
    }
}

android {
    namespace = "${Config.APPLICATION_ID}.data"
    compileSdk = Config.COMPILE_SDK
    defaultConfig { minSdk = Config.MIN_SDK }
    compileOptions {
        sourceCompatibility = Config.JAVA_VERSION
        targetCompatibility = Config.JAVA_VERSION
    }
}
