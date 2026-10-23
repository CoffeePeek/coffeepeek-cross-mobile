package com.coffeepeek.feature.appupdate.impl

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.appupdate.api.AppUpdateEntry
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.ui.AppUpdatePrompt

/**
 * [installationEffect] hosts the platform side of the installer (Play/APK launchers and the snackbar flow) and still lives in the
 * application until the installers move into this module.
 */
fun createAppUpdateEntry(
    controller: AppUpdateController,
    installer: UpdateInstaller,
    environment: AppUpdateEnvironment,
    installationEffect: @Composable () -> Unit,
): AppUpdateEntry = object : AppUpdateEntry {
    @Composable
    override fun Content() = AppUpdatePrompt(controller, installer, environment, installationEffect)
}
