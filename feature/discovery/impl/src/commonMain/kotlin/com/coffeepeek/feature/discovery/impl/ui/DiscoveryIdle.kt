package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.search.SearchState

/** Shared idle criteria exclude refresh, filter and location work from review prompts. */
internal fun SearchState.isIdleDiscoveryContent(): Boolean =
    showsDiscovery && !isLoading && !isRefreshing && error == null && !showFilters &&
        !nearbyRequested && !locationUnavailable && !locationSettingsNeeded
