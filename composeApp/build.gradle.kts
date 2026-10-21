import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.VerifyApkRuntimeClassesTask
import com.android.build.api.artifact.SingleArtifact
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
    id("com.google.gms.google-services") version "4.5.0" apply false
}

if (file("google-services.json").isFile) apply(plugin = "com.google.gms.google-services")

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
            implementation(project(module.feature.shop.domain))
            implementation(project(module.feature.shop.data))
            implementation(project(module.feature.shop.impl))
            implementation(project(module.core.coroutines))
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.concurrent.futures)
            implementation(libs.androidx.appcompat)
            implementation("androidx.core:core-splashscreen:1.0.1")
            implementation("androidx.exifinterface:exifinterface:1.4.1")
            implementation(libs.maplibre.android)
            implementation("com.google.android.gms:play-services-auth:21.3.0")
            // Match Espresso's annotations under Android's consistent runtime/test resolution.
            implementation(libs.errorprone.annotations)
            implementation("com.google.android.play:app-update:2.1.0")
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:35.0.2"))
            implementation("com.google.firebase:firebase-messaging")
            implementation("com.microsoft.signalr:signalr:10.0.9")
            implementation("org.slf4j:slf4j-nop:2.0.16")
        }
        commonMain.dependencies {
            implementation(project(module.core.designSystem))
            implementation(project(module.feature.favorites.domain))
            implementation(libs.posthog.kmp)
            implementation(project(module.legacy.domain))
            implementation(project(module.legacy.data))
            implementation(project(module.legacy.network))
            implementation(project(module.legacy.room))
            implementation(project(module.core.network))
            implementation(project(module.feature.homeCheckIn))
            implementation(project(module.feature.authorBlocking))
            implementation(project(module.feature.notifications))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.ui.backhandler)
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
            implementation(libs.kotlinx.coroutines.test)
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
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

val apiBaseUrl: String = providers.gradleProperty("apiBaseUrl").orNull ?: run {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val props = Properties()
        localPropertiesFile.inputStream().use { props.load(it) }
        props.getProperty("API_BASE_URL", "")
    } else {
        ""
    }
}

val postHogProjectToken = providers.environmentVariable("POSTHOG_PROJECT_TOKEN")
    .orElse(providers.provider {
        val props = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
        props.getProperty("POSTHOG_PROJECT_TOKEN", "")
    })
    .map { value ->
        value.trim().also { require(it.isEmpty() || it.matches(Regex("phc_[A-Za-z0-9_-]+"))) {
            "POSTHOG_PROJECT_TOKEN must be a PostHog project ingestion token"
        } }
    }

android {
    namespace = Config.APPLICATION_ID
    // Navigation 3 1.2.0 in the Android-only favorites feature requires API 37.
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }
    if (providers.gradleProperty("pushTestApp").orNull == "true") {
        buildTypes.getByName("debug") { applicationIdSuffix = ".pushdebug" }
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
        buildConfigField("boolean", "POSTHOG_ENABLE_DEV", providers.gradleProperty("posthogEnableDev").map { (it == "true").toString() }.getOrElse("false"))
        buildConfigField("String", "POSTHOG_PROJECT_TOKEN", "\"${postHogProjectToken.get()}\"")
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
    // Plain Compose test host: no MainActivity startup/splash or real root navigation.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Debug variants are not minified: startup DI classes must exist as definitions in the APK.
androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }
        val verification = tasks.register<VerifyApkRuntimeClassesTask>("verify${variantName}RuntimeClasses") {
            group = "verification"
            description = "Checks startup class definitions inside the ${variant.name} APK."
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            requiredClasses.set(listOf(
                "com.coffeepeek.api.service.CheckInApiService",
                "com.coffeepeek.api.CoffeePeekRepo",
                "com.coffeepeek.data.di.DataModuleKt",
                "com.coffeepeek.data.feature.favorites.ServerFavoritesRepository",
                "com.coffeepeek.admin.di.favorites.ServerFavoriteShopsRepository",
                "com.coffeepeek.admin.di.favorites.ServerFavoriteDrinksRepository",
                "com.coffeepeek.admin.di.shop.ShopCheckInModuleKt",
                "com.coffeepeek.admin.di.shop.AndroidShopCheckInFormRenderer",
                "com.coffeepeek.admin.di.shop.AndroidSessionShopCheckInEntry",
                "com.coffeepeek.admin.di.shop.AndroidShopCheckInSession",
                "com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry",
                "com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository",
                "com.coffeepeek.feature.shop.data.repository.ShopCheckInRepositoryFactoryKt",
                "com.coffeepeek.feature.shop.impl.ShopCheckInCreateApiImplKt",
            ))
        }
        tasks.matching { it.name == "assemble$variantName" }.configureEach { finalizedBy(verification) }
    }
}
