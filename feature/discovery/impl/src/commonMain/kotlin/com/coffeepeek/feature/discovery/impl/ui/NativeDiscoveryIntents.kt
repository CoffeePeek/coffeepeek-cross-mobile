package com.coffeepeek.feature.discovery.impl.ui

// Native presentation intents stay shared; platform UI does not filter accepted results.

import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.matchesPickerQuery

internal fun discoveryIosSearchIntent(name: String, id: String, state: SearchState, picker: Boolean = false): SearchAction? = when (name) {
    "activate" -> SearchAction.ActivateSearch
    "cancel" -> SearchAction.CancelSearch
    "filters" -> SearchAction.ToggleFilters
    "refresh" -> SearchAction.Refresh
    "more" -> SearchAction.LoadMore.takeIf { state.canLoadMore && (!picker || iosPickerQueryAccepted(state)) }
    "retry-more" -> SearchAction.RetryLoadMore
    "reset" -> SearchAction.ResetFilterSelections
    "open" -> SearchAction.OpenShop(id).takeIf { id in iosAcceptedShopIds(state) }
    "favorite" -> SearchAction.ToggleFavorite(id).takeIf { state.visibleShops.any { it.id == id } }
    "dismiss-favorite" -> state.favoriteFailureId?.let(SearchAction::DismissFavoriteFailure)
    "dismiss-location" -> SearchAction.DismissLocationFailure
    else -> discoveryIosFilterIntent(name, state)
}

private fun discoveryIosFilterIntent(name: String, state: SearchState): SearchAction? = when (name) {
    "nearby" -> SearchAction.ToggleNearby
    "open-only" -> SearchAction.ToggleOpen
    "new" -> SearchAction.ToggleNew
    "visited" -> SearchAction.ToggleVisited
    "favorites" -> SearchAction.ToggleFavorites
    else -> ShopKind.entries.firstOrNull { name == it.name }?.let {
        SearchAction.SetKind(it.takeUnless { kind -> kind == state.filters.kind })
    }
}

internal fun discoveryIosMapIntent(name: String, id: String): MapAction? = when (name) {
    "suggestion" -> MapAction.SelectSuggestion(id)
    "map-open" -> MapAction.OpenShop(id)
    "map-select" -> MapAction.SelectMarker(id)
    "carousel" -> MapAction.SelectFromCarousel(id)
    "zones" -> MapAction.ToggleZones
    "heatmap" -> MapAction.ToggleHeatmap
    "location" -> MapAction.RequestMyLocation
    "zoom-in" -> MapAction.ZoomIn
    "zoom-out" -> MapAction.ZoomOut
    "retry-map" -> MapAction.RetryViewport
    "zone-shops" -> MapAction.ShowZoneShops
    "zone-close" -> MapAction.ClearZone
    "dismiss-map-location" -> MapAction.DismissLocationSettings
    else -> null
}

internal fun iosPickerQueryAccepted(state: SearchState): Boolean =
    state.pickerResultCriteria?.matchesPickerQuery(state.criteria) == true

private fun iosAcceptedShopIds(state: SearchState): Set<String> =
    (state.visibleShops + state.pickerRecentShops).mapTo(mutableSetOf()) { it.id }
