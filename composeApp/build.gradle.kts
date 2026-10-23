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
        // Fail the build instead of shipping runtime IrLinkageError stubs to TestFlight.
        iosTarget.compilerOptions.freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR")
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
            implementation(project(module.feature.profile.impl))
            implementation(project(module.feature.settings.impl))
            implementation(project(module.feature.coffee.api))
            implementation(project(module.feature.coffee.domain))
            implementation(project(module.feature.coffee.data))
            implementation(project(module.feature.coffee.impl))
            implementation(project(module.feature.checkInHistory.api))
            implementation(project(module.feature.checkInHistory.domain))
            implementation(project(module.feature.checkInHistory.data))
            implementation(project(module.feature.checkInHistory.impl))
            implementation(project(module.feature.homePreparation.domain))
            implementation(project(module.feature.homePreparation.api))
            implementation(project(module.feature.homePreparation.data))
            implementation(project(module.feature.homePreparation.impl))
            implementation(project(module.core.coroutines))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.concurrent.futures)
            implementation(libs.androidx.appcompat)
            implementation("androidx.core:core-splashscreen:1.0.1")
            implementation("androidx.exifinterface:exifinterface:1.4.1")
            implementation(libs.maplibre.android)
            implementation("com.google.android.gms:play-services-auth:21.3.0")
            // Match Espresso's annotations under Android's consistent runtime/test resolution.
            implementation(libs.errorprone.annotations)
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:35.0.2"))
            implementation("com.google.firebase:firebase-messaging")
            implementation("com.microsoft.signalr:signalr:10.0.9")
            implementation("org.slf4j:slf4j-nop:2.0.16")
        }
        commonMain.dependencies {
            implementation(project(module.feature.profile.api))
            implementation(project(module.feature.profile.domain))
            implementation(project(module.feature.profile.data))
            implementation(project(module.feature.settings.api))
            implementation(project(module.feature.settings.domain))
            implementation(project(module.feature.roaster.api))
            implementation(project(module.feature.roaster.domain))
            implementation(project(module.feature.roaster.data))
            implementation(project(module.feature.roaster.impl))
            implementation(project(module.core.designSystem))
            implementation(project(module.core.session))
            implementation(project(module.core.datetime))
            implementation(project(module.feature.favorites.domain))
            implementation(libs.posthog.kmp)
            implementation(project(module.legacy.domain))
            implementation(project(module.legacy.data))
            implementation(project(module.legacy.network))
            implementation(project(module.legacy.room))
            implementation(project(module.core.network))
            implementation(project(module.legacy.homeCheckIn))
            implementation(project(module.feature.appUpdate.api))
            implementation(project(module.feature.auth.api))
            implementation(project(module.feature.community.api))
            implementation(project(module.feature.community.domain))
            implementation(project(module.feature.community.data))
            implementation(project(module.feature.community.impl))
            implementation(project(module.feature.contributions.api))
            implementation(project(module.feature.contributions.domain))
            implementation(project(module.feature.contributions.data))
            implementation(project(module.feature.contributions.impl))
            implementation(project(module.feature.discovery.api))
            implementation(project(module.feature.discovery.domain))
            implementation(project(module.feature.discovery.data))
            implementation(project(module.feature.discovery.impl))
            implementation(project(module.feature.auth.domain))
            implementation(project(module.feature.auth.data))
            implementation(project(module.feature.auth.impl))
            implementation(project(module.feature.appUpdate.domain))
            implementation(project(module.feature.appUpdate.data))
            implementation(project(module.feature.appUpdate.impl))
            implementation(project(module.feature.authorBlocking.api))
            implementation(project(module.feature.authorBlocking.impl))
            implementation(project(module.feature.notifications.api))
            implementation(project(module.feature.notifications.impl))
            implementation(project(module.feature.notifications.data))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.preview)
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
        // Test the exact sandbox transport without adding it to the live APK or requiring a new test variant.
        androidUnitTest {
            kotlin.srcDir("src/androidSandbox/kotlin/com/coffeepeek/admin/sandbox/backend")
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
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }

val appVersionConfig = providers.fileContents(
    rootProject.layout.projectDirectory.file("iosApp/Configuration/Shared.xcconfig")
).asText
val appBuildNumberOffset = appVersionConfig.map { config ->
    Regex("(?m)^BUILD_NUMBER_OFFSET = ([0-9]+)$")
        .find(config)?.groupValues?.get(1)?.toInt() ?: error("BUILD_NUMBER_OFFSET is required")
}
val appVersionCode: Provider<Int> = gitCommitCount.zip(appBuildNumberOffset) { count, offset -> count + offset }
val appVersionName: Provider<String> = appVersionCode.map { "1.1.1.$it" }

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
        getByName("debug") {
            // Normal backend/session, but no release OAuth flow in debug builds.
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"\"")
        }
        create("sandbox") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".sandbox"
            versionNameSuffix = "-sandbox"
            matchingFallbacks += "debug"
            buildConfigField("boolean", "APK_UPDATES_ENABLED", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://sandbox.coffeepeek.invalid\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"\"")
            buildConfigField("String", "POSTHOG_PROJECT_TOKEN", "\"\"")
            buildConfigField("boolean", "POSTHOG_ENABLE_DEV", "false")
        }
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

kotlin.applyDefaultHierarchyTemplate()
kotlin.sourceSets {
    val androidSandbox = create("androidSandbox") {
        dependsOn(getByName("androidMain"))
        dependencies { implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}") }
    }
    matching { it.name in setOf("androidDirectSandbox", "androidPlaySandbox") }.all {
        dependsOn(androidSandbox)
    }
}

// Reuse the app graph with isolated ports, never the production Application/services.
androidComponents.onVariants(androidComponents.selector().withBuildType("sandbox")) { variant ->
    tasks.matching { it.name == "process${variant.name.replaceFirstChar { it.uppercaseChar() }}GoogleServices" }
        .configureEach { enabled = false }
    val runtimeVerification = tasks.register<VerifyApkRuntimeClassesTask>("verify${variant.name.replaceFirstChar { it.uppercaseChar() }}RuntimeClasses") {
        apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
        requiredClasses.set(listOf("com.coffeepeek.admin.sandbox.SandboxActivity",
            "com.coffeepeek.admin.sandbox.backend.SandboxBackend", "com.coffeepeek.admin.sandbox.SandboxApplication",
            "com.coffeepeek.feature.homecheckin.data.repository.HomePreparationRepositoryFactoryKt",
            "com.coffeepeek.admin.ui.Navigator"))
    }
    val manifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
    val verification = tasks.register("verify${variant.name.replaceFirstChar { it.uppercaseChar() }}Isolation") {
        inputs.file(manifest)
        doLast {
            val xml = manifest.get().asFile.readText()
            check("android.permission.INTERNET" !in xml) { "Sandbox must not have network permission" }
            check("com.coffeepeek.admin.MainActivity" !in xml) { "Sandbox must not launch the production root" }
            check("com.coffeepeek.admin.CoffeePeekApplication" !in xml) { "Sandbox must not start production DI/services" }
            check("com.google.firebase.provider.FirebaseInitProvider" !in xml) { "Sandbox must not initialize Firebase" }
            check("com.coffeepeek.admin.notifications.CoffeePeekMessagingService" !in xml) { "Sandbox must not start production push" }
            check("com.coffeepeek.admin.sandbox.SandboxActivity" in xml) { "Missing Sandbox entry" }
            check("com.coffeepeek.admin.sandbox.SandboxApplication" in xml) { "Missing isolated Sandbox bootstrap" }
        }
    }
    tasks.matching { it.name == "assemble${variant.name.replaceFirstChar { it.uppercaseChar() }}" }.configureEach {
        finalizedBy(verification, runtimeVerification)
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
            forbiddenClassPrefixes.set(listOf("com.coffeepeek.admin.sandbox.", "io.ktor.client.engine.mock."))
            requiredClasses.set(listOf(
                "com.coffeepeek.api.service.CheckInApiService",
                "com.coffeepeek.api.CoffeePeekRepo",
                "com.coffeepeek.data.di.DataModuleKt",
                "com.coffeepeek.admin.di.roaster.RoasterModuleKt",
                "com.coffeepeek.admin.di.roaster.FeatureRoasterScreenRenderer",
                "com.coffeepeek.feature.roaster.impl.RoasterEntryImplsKt",
                "com.coffeepeek.feature.roaster.data.repository.RoasterRepositoryFactoryKt",
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
                "com.coffeepeek.admin.di.checkinhistory.CheckInHistoryModuleKt",
                "com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistoryScreenRenderer",
                "com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession",
                "com.coffeepeek.admin.di.checkinhistory.AndroidHistoryHomeBridge",
                "com.coffeepeek.admin.di.checkinhistory.HistoryHomeEditorFactoryKt",
                "com.coffeepeek.feature.checkinhistory.api.CheckInHistoryEntry",
                "com.coffeepeek.feature.checkinhistory.data.repository.CheckInHistoryRepositoryFactoryKt",
                "com.coffeepeek.feature.checkinhistory.impl.CheckInHistoryApiImplKt",
                "com.coffeepeek.feature.checkinhistory.impl.ui.CombinedHistoryViewModel",
                "com.coffeepeek.feature.homecheckin.data.repository.HomePreparationRepositoryFactoryKt",
                "com.coffeepeek.admin.di.checkinhistory.HomeHistoryReaderAdapter",
                "com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader",
                "com.coffeepeek.feature.homecheckin.domain.model.HomePreparation",
                "com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry",
                "com.coffeepeek.feature.homecheckin.api.HomeCheckInDetailEntry",
                "com.coffeepeek.feature.homecheckin.impl.HomeCheckInEditorApiImplKt",
                "com.coffeepeek.feature.homecheckin.impl.HomeCheckInApiImplKt",
                "com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInEditorViewModel",
                "com.coffeepeek.admin.di.homecheckin.AndroidHomeCheckInEditorKt",
                "com.coffeepeek.admin.di.homecheckin.AndroidHomeCheckInRepository",
                "com.coffeepeek.admin.di.homecheckin.AndroidHomeCheckInPhotoPicker",
                "com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer",
                "com.coffeepeek.admin.di.homecheckin.HomeCheckInModuleKt",
                "com.coffeepeek.admin.di.homecheckin.AndroidCheckInCreateFlowRenderer",
                "com.coffeepeek.admin.di.homecheckin.AndroidGlobalCheckInFlow",
                "com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader",
            ))
        }
        tasks.matching { it.name == "assemble$variantName" }.configureEach { finalizedBy(verification) }
    }
}
