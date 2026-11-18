package com.coffeepeek.admin.di.discovery

import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosState
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosShop
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosChip
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosZone
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosShopDecoration

internal fun DiscoveryIosState.toNative() = IosDiscoveryState(
    picker, active, query,
    landing, mapExpanded, roastersExpanded,
    showRoasterResults, canSuggestRoaster, scrollRevision,
    bottomClearance, loading, refreshing,
    loadingMore, canLoadMore, failed,
    loadMoreFailed, canSuggestShop, emptyReason,
    locationNotice, settingsNeeded, favoriteFailure,
    continueCheckIn, shops.map { it.toNative() }, recent.map { it.toNative() },
    mapShops.map { it.toNative() }, selectedShopId, suggestions.map { it.toNative() },
    showSuggestions, searchFailed, searchLoading,
    zone?.toNative(), zonesVisible, heatmapVisible,
    heatmapLoading, heatmapFailed, viewportFailed,
    truncated, chips.map { it.toNative() }, activeFilterCount,
    labels,
)

internal fun DiscoveryIosShop.toNative() = IosDiscoveryShop(
    id, title, photoUrl,
    address, rating, detail,
    distance, kind, tags,
    favorite, favoriteBusy, favoriteLabel,
    open, isNew, hours,
    decoration?.toNative(),
)

internal fun DiscoveryIosChip.toNative() = IosDiscoveryChip(
    id, title, selected,
)

internal fun DiscoveryIosZone.toNative() = IosDiscoveryZone(
    title, description, count,
)

internal fun DiscoveryIosShopDecoration.toNative() = IosDiscoveryShopDecoration(
    priceLevel, ratingCount, brewMethods,
    roasterLogos,
)
