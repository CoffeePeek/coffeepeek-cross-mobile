package com.coffeepeek.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/** Shared presentation/Native targets, with Compose UI and its dependencies only on Android. */
class KmpSharedAndroidComposeLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.coffeepeek.kmp.shared-library")
        project.pluginManager.apply("com.coffeepeek.kmp.android-compose-library")
        project.extensions.getByType(ComposeCompilerGradlePluginExtension::class.java)
            .targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
    }
}
