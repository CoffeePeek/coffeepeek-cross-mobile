package com.coffeepeek.admin.di.appupdate

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateRenderer
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CpSnackbarHost
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.feature.appupdate.api.AppUpdateEntry
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.appupdate.impl.createAppUpdateEntry

/** Android composes the migrated prompt; the snackbar look and the floating-navigation clearance come from the application. */
internal class AndroidAppUpdateRenderer(private val entry: AppUpdateEntry) : AppUpdateRenderer {
    @Composable
    override fun Prompt() = entry.Content()
}

internal fun androidAppUpdateRenderer(controller: AppUpdateController, installer: AndroidUpdateInstaller): AppUpdateRenderer =
    AndroidAppUpdateRenderer(
        createAppUpdateEntry(
            controller, installer, AppConfigUpdateEnvironment,
            snackbarHost = { hostState, showRetryIcon, actionInProgress, onAction ->
                CpSnackbarHost(
                    hostState = hostState,
                    actionIcon = CpIcons.Retry.takeIf { showRetryIcon },
                    actionInProgress = actionInProgress,
                    onAction = onAction,
                )
            },
            extraBottomClearance = CpDimens.floatingNavContentClearance + CpDimens.spacing2,
        ),
    )
