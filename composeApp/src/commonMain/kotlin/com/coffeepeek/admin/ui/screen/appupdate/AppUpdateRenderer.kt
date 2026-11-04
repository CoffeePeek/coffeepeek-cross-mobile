package com.coffeepeek.admin.ui.screen.appupdate

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface AppUpdateRenderer {
    @Composable
    fun Prompt()
}
