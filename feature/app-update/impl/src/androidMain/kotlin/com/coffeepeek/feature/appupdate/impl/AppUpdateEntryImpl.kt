package com.coffeepeek.feature.appupdate.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.coffeepeek.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.feature.appupdate.api.AppUpdateEntry
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.impl.ui.AppUpdatePrompt
import com.coffeepeek.feature.appupdate.impl.ui.UpdateInstallationEffect
import com.coffeepeek.feature.appupdate.impl.ui.UpdateSnackbarHost

/**
 * The root update prompt with the platform side of the installer (Play/APK launchers and the Play snackbar flow).
 * [snackbarHost] is the app's snackbar presentation; [extraBottomClearance] keeps it above the floating navigation.
 */
fun createAppUpdateEntry(
    controller: AppUpdateController,
    installer: AndroidUpdateInstaller,
    environment: AppUpdateEnvironment,
    snackbarHost: UpdateSnackbarHost,
    extraBottomClearance: Dp,
): AppUpdateEntry = object : AppUpdateEntry {
    @Composable
    override fun Content() = AppUpdatePrompt(controller, installer, environment) {
        UpdateInstallationEffect(installer, controller, environment, snackbarHost, extraBottomClearance)
    }
}
