package com.coffeepeek.admin.di.appupdate

import com.coffeepeek.admin.ui.NavMetrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.screen.appupdate.AppUpdateRenderer
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.admin.ui.component.CpSnackbarHost
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
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
                    actionIcon = CoffeePeekIcons.Retry.takeIf { showRetryIcon },
                    actionInProgress = actionInProgress,
                    onAction = onAction,
                )
            },
            extraBottomClearance = maxOf(
                NavMetrics.floatingNavContentClearance + CoffeePeekDimens.spacing2,
                NavMetrics.floatingNavClearance(0.dp),
            ),
        ),
    )
