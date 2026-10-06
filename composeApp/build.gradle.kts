import com.coffeepeek.buildlogic.module
import com.coffeepeek.config.Config
import com.coffeepeek.config.PrintValueTask
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION))
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(module.feature.favorites.api))
            implementation(project(module.feature.favorites.data))
            implementation(project(module.feature.favorites.impl))
            implementation(project(module.feature.shopReport.api))
            implementation(project(module.feature.shopReport.data))
            implementation(project(module.feature.shopReport.impl))
            implementation(project(module.feature.shop.api))
            implementation(project(module.feature.shop.data))
            implementation(project(module.feature.shop.impl))
            implementation(project(module.core.coroutines))
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation("androidx.core:core-splashscreen:1.0.1")
            implementation("androidx.exifinterface:exifinterface:1.4.1")
            implementation(libs.maplibre.android)
            implementation("com.google.android.gms:play-services-auth:21.3.0")
            implementation("com.google.android.play:app-update:2.1.0")
            implementation("com.microsoft.signalr:signalr:10.0.9")
            implementation("org.slf4j:slf4j-nop:2.0.16")
        }
        commonMain.dependencies {
            implementation(project(module.feature.favorites.domain))
            implementation(project(module.legacy.domain))
            implementation(project(module.legacy.data))
            implementation(project(module.legacy.network))
            implementation(project(module.legacy.room))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.phosphor.icon)
            implementation(libs.haze)

            implementation(libs.androidx.navigation)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kamel)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.espresso.core)
        }
    }
}

val gitCommitCount: Provider<Int> = providers.exec {
    commandLine("git", "rev-list", "--all", "--count", "HEAD")
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }

val appVersionCode: Provider<Int> = gitCommitCount
val appVersionName: Provider<String> = gitCommitCount.map { "1.0.$it" }

tasks.register<PrintValueTask>("printVersionName") {
    group = "versioning"
    description = "Prints the Android version name."
    value.set(appVersionName)
}

tasks.register<PrintValueTask>("printVersionCode") {
    group = "versioning"
    description = "Prints the Android version code."
    value.set(appVersionCode.map(Int::toString))
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.orNull.isNullOrBlank() }

val googleWebClientId: String = run {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val props = Properties()
        localPropertiesFile.inputStream().use { props.load(it) }
        props.getProperty("GOOGLE_WEB_CLIENT_ID", "")
    } else {
        ""
    }
}

val apiBaseUrl: String = run {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val props = Properties()
        localPropertiesFile.inputStream().use { props.load(it) }
        props.getProperty("API_BASE_URL", "")
    } else {
        ""
    }
}

android {
    namespace = Config.APPLICATION_ID
    // Navigation 3 1.2.0 in the Android-only favorites feature requires API 37.
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = Config.APPLICATION_ID
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = Config.MIN_SDK
        targetSdk = Config.TARGET_SDK
        versionCode = appVersionCode.get()
        versionName = appVersionName.get()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    flavorDimensions += "delivery"
    productFlavors {
        create("play") {
            dimension = "delivery"
            buildConfigField("boolean", "APK_UPDATES_ENABLED", "false")
        }
        create("direct") {
            dimension = "delivery"
            buildConfigField("boolean", "APK_UPDATES_ENABLED", "true")
        }
    }
    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = Config.JAVA_VERSION
        targetCompatibility = Config.JAVA_VERSION
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
