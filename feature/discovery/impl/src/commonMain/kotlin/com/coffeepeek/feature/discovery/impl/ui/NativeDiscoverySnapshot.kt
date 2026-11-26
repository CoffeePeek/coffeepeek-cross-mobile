package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.api.DiscoveryRoasterContext
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters

internal data class NativeDiscoveryOverlay(val active: Boolean = false, val mapExpanded: Boolean = false,
    val roastersExpanded: Boolean = false, val scrollRevision: Int = 0, val bottomClearance: Float = 0f,
    val canContinue: Boolean = false, val hasRoasters: Boolean? = null)

internal data class NativeDiscoverySnapshot(val search: SearchState, val map: MapState,
    val overlay: NativeDiscoveryOverlay, val reviewEligible: Boolean) {
    val roasterContext get() = DiscoveryRoasterContext(search.query, search.filters.roasterIds, search.filters.favoritesOnly)
}

internal data class NativeDiscoveryFilterSnapshot(val search: SearchState, val filters: DiscoveryFilters)

data class NativeDiscoveryRuntimeState(val roasters: DiscoveryRoasterContext, val active: Boolean,
    val searchPermission: Int, val mapPermission: Int, val reviewEligible: Boolean)
