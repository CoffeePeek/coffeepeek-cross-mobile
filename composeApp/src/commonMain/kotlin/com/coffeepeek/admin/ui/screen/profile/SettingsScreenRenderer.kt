package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface SettingsScreenRenderer {
    @Composable
    fun Tab()

    @Composable
    fun Theme()

    @Composable
    fun City()

    @Composable
    fun UsagePolicy()
}
