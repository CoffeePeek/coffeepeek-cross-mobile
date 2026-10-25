package com.coffeepeek.admin.ui.screen.main

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface DiscoveryScreenRenderer {
    /** [onMapExpandedChange] tells the host that the map covers the screen, so it can hide the floating navigation. */
    @Composable
    fun Tab(onMapExpandedChange: (Boolean) -> Unit)
}
