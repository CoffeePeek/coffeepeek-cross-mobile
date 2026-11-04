package com.coffeepeek.admin.ui.screen.main

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Platform selection only. iOS and shared code keep the legacy discovery tab; Android composes the migrated entry. */
interface DiscoveryScreenRenderer {
    /** [onMapExpandedChange] tells the host that the map covers the screen, so it can hide the floating navigation. */
    @Composable
    fun Tab(onMapExpandedChange: (Boolean) -> Unit, reselections: Flow<Unit> = emptyFlow())
}

internal object LegacyDiscoveryScreenRenderer : DiscoveryScreenRenderer {
    @Composable override fun Tab(onMapExpandedChange: (Boolean) -> Unit, reselections: Flow<Unit>) =
        DiscoveryTabContent(onMapExpandedChange, reselections)
}
