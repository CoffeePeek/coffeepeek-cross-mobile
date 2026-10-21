package com.coffeepeek.feature.appupdate.api

import androidx.compose.runtime.Composable

/**
 * The root update prompt. It checks for an update on every resume of the host, shows the card or snackbar and drives the installer.
 * The caller composes it once, next to the navigation host.
 */
interface AppUpdateEntry {
    @Composable
    fun Content()
}
