package com.coffeepeek.buildlogic

import com.android.build.gradle.LibraryExtension
import com.coffeepeek.config.Config
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Standard Android target, SDK and Java/Kotlin toolchain for a KMP library. */
class KmpAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("com.android.library")

        val module = project.extensions.create("coffeepeekModule", CoffeePeekModuleExtension::class.java)
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.androidTarget {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION))
        }

        val android = project.extensions.getByType(LibraryExtension::class.java)
        android.compileSdk = Config.COMPILE_SDK
        android.defaultConfig.minSdk = Config.MIN_SDK
        android.defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        android.compileOptions.sourceCompatibility = Config.JAVA_VERSION
        android.compileOptions.targetCompatibility = Config.JAVA_VERSION
        module.attach(android)

        project.afterEvaluate {
            check(!android.namespace.isNullOrBlank()) {
                "Set android.namespace for ${project.path}"
            }
        }
    }
}

/** Shared feature/core library with the supported iOS framework targets configured. */
class KmpSharedLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.coffeepeek.kmp.android-library")
        project.extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
            iosArm64()
            iosSimulatorArm64()
        }
    }
}

/** Android Compose feature library, including the Compose compiler plugins. */
class KmpAndroidComposeLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.coffeepeek.kmp.android-library")
        project.pluginManager.apply("org.jetbrains.compose")
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        project.extensions.getByType(LibraryExtension::class.java).compileSdk = 37

        val android = project.extensions.getByType(LibraryExtension::class.java)
        val compose = project.extensions.getByType(ComposeExtension::class.java)
        val resources = compose.extensions.getByType(ResourcesExtension::class.java)
        project.afterEvaluate {
            resources.packageOfResClass = "${android.namespace}.resources"
            resources.publicResClass = false
        }
    }
}
