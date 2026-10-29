package com.coffeepeek.admin.feature.appupdate.ui

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface AppUpdateRenderer {
    @Composable
    fun Prompt()
}
