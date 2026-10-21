package com.coffeepeek.admin.feature.appupdate.ui

import androidx.compose.runtime.Composable

/** Platform selection only. iOS keeps the legacy prompt; Android composes the migrated entry. */
interface AppUpdateRenderer {
    @Composable
    fun Prompt()
}

internal object LegacyAppUpdateRenderer : AppUpdateRenderer {
    @Composable
    override fun Prompt() = AppUpdatePrompt()
}
