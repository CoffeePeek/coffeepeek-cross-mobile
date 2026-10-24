package com.coffeepeek.admin.ui.screen.main

import androidx.compose.runtime.Composable

/** Platform selection only. Android and iOS bind the migrated entry; the legacy tab remains a fallback. */
interface DiscoveryScreenRenderer {
    /** [onMapExpandedChange] tells the host that the map covers the screen, so it can hide the floating navigation. */
    @Composable
    fun Tab(onMapExpandedChange: (Boolean) -> Unit)
}

internal object LegacyDiscoveryScreenRenderer : DiscoveryScreenRenderer {
    @Composable override fun Tab(onMapExpandedChange: (Boolean) -> Unit) = DiscoveryTabContent(onMapExpandedChange)
}
