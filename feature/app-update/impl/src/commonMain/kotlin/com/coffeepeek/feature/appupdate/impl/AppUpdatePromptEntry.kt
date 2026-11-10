package com.coffeepeek.feature.appupdate.impl

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.appupdate.api.AppUpdateEntry
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.ui.AppUpdatePrompt

/**
 * The root update prompt for a platform whose installer needs no composition side (the App Store link on iOS).
 * [installationEffect] runs before the prompt for platforms that do need one.
 */
fun createAppUpdatePromptEntry(
    controller: AppUpdateController,
    installer: UpdateInstaller,
    environment: AppUpdateEnvironment,
    installationEffect: @Composable () -> Unit = {},
): AppUpdateEntry = object : AppUpdateEntry {
    @Composable
    override fun Content() = AppUpdatePrompt(controller, installer, environment, installationEffect)
}
