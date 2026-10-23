package com.coffeepeek.admin.di.appupdate

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateRenderer
import com.coffeepeek.admin.feature.appupdate.ui.UpdateInstallationEffect
import com.coffeepeek.feature.appupdate.api.AppUpdateEntry
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.appupdate.impl.createAppUpdateEntry

/** Android composes the migrated prompt; the installer side effect still comes from the application. */
internal class AndroidAppUpdateRenderer(private val entry: AppUpdateEntry) : AppUpdateRenderer {
    @Composable
    override fun Prompt() = entry.Content()
}

internal fun androidAppUpdateRenderer(controller: AppUpdateController, installer: UpdateInstaller): AppUpdateRenderer =
    AndroidAppUpdateRenderer(createAppUpdateEntry(controller, installer, AppConfigUpdateEnvironment) { UpdateInstallationEffect() })
