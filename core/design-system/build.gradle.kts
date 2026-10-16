import com.coffeepeek.config.Config
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION)) }
    }
    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.ui)
            api(compose.foundation)
            api(compose.material3)
            api(libs.haze)
            implementation(libs.phosphor.icon)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.espresso.core)
        }
    }
}

compose.resources {
    packageOfResClass = "com.coffeepeek.core.designsystem.resources"
    publicResClass = false
}

dependencies {
    add("debugImplementation", compose.uiTooling)
}

android {
    namespace = "${Config.APPLICATION_ID}.core.designsystem"
    compileSdk = Config.COMPILE_SDK
    defaultConfig {
        minSdk = Config.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = Config.JAVA_VERSION
        targetCompatibility = Config.JAVA_VERSION
    }
}
