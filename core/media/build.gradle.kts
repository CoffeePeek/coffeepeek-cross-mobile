import com.coffeepeek.config.Config
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION)) } }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            api(project(":core:session"))
            api(libs.kamel)
            api(compose.runtime)
            api(compose.ui)
            implementation(compose.foundation)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation("androidx.exifinterface:exifinterface:1.4.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
        }
    }
}

android {
    namespace = "${Config.APPLICATION_ID}.core.media"
    compileSdk = Config.COMPILE_SDK
    defaultConfig { minSdk = Config.MIN_SDK }
    compileOptions {
        sourceCompatibility = Config.JAVA_VERSION
        targetCompatibility = Config.JAVA_VERSION
    }
}
