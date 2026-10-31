package com.coffeepeek.feature.discovery.impl.tab

internal enum class DiscoveryBackTarget { Roasters, Map, Search }

/** IME and modal windows handle Back first; these are the remaining states inside the tab. */
internal fun discoveryBackTarget(
    showRoasters: Boolean,
    mapExpanded: Boolean,
    showDiscovery: Boolean,
): DiscoveryBackTarget? = when {
    showRoasters -> DiscoveryBackTarget.Roasters
    mapExpanded -> DiscoveryBackTarget.Map
    !showDiscovery -> DiscoveryBackTarget.Search
    else -> null
}
