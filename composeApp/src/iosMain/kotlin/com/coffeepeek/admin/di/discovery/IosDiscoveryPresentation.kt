package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.di.roaster.IosRoasterDiscovery

/** A mode and its prepared native child are published together to the Swift surface. */
data class IosDiscoveryPresentation(
    val state: IosDiscoveryState,
    val roasters: IosRoasterDiscovery?,
    val ready: Boolean,
)
