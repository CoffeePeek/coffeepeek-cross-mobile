package com.coffeepeek.admin.di.appupdate

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateRenderer
import com.coffeepeek.feature.appupdate.api.AppUpdateEntry
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.appupdate.impl.createAppUpdatePromptEntry

/** The migrated update prompt for a platform whose installer needs no composition side (the App Store link on iOS). */
internal class FeatureAppUpdateRenderer(private val entry: AppUpdateEntry) : AppUpdateRenderer {
    @Composable
    override fun Prompt() = entry.Content()
}

internal fun featureAppUpdateRenderer(controller: AppUpdateController, installer: UpdateInstaller): AppUpdateRenderer =
    FeatureAppUpdateRenderer(createAppUpdatePromptEntry(controller, installer, AppConfigUpdateEnvironment))
