pluginManagement {
    includeBuild("build-logic")
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application",
                "com.android.library" ->
                    useModule("com.android.tools.build:gradle:${requested.version}")
                "org.jetbrains.kotlin.multiplatform" ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
                "org.jetbrains.kotlin.plugin.compose" ->
                    useModule("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${requested.version}")
                "org.jetbrains.kotlin.plugin.serialization" ->
                    useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
                "org.jetbrains.compose" ->
                    useModule("org.jetbrains.compose:compose-gradle-plugin:${requested.version}")
                "com.google.devtools.ksp" ->
                    useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
                "androidx.room" ->
                    useModule("androidx.room:room-gradle-plugin:${requested.version}")
            }
        }
    }
    repositories {
        maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/public/p/compose/dev")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.coffeepeek.modules")
}

rootProject.name = "CoffeePeek"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// xcross discovers the iOS framework from literal includes without evaluating settings plugins.
// Re-including the app is idempotent; com.coffeepeek.modules still owns the module graph.
include(":composeApp")

dependencyResolutionManagement {
    repositories {
        maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/public/p/compose/dev")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}
