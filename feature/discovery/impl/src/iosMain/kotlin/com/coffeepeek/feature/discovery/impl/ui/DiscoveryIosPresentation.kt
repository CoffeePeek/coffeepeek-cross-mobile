package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.api.MapCanvasState
import com.coffeepeek.feature.discovery.api.MapCanvasCallbacks
import com.coffeepeek.feature.discovery.api.MapCamera

/** Native UI receives rendering values and intents; Kotlin retains the existing feature models. */
class DiscoveryIosPresentation internal constructor(
    val state: DiscoveryIosState,
    val canvas: MapCanvasState,
    val canvasCallbacks: MapCanvasCallbacks,
    val viewportCamera: MapCamera?,
    val onAction: (String, String) -> Unit,
    val onQuery: (String) -> Unit,
    val openLocationSettings: () -> Unit,
)

data class DiscoveryIosState(
    val picker: Boolean,
    val active: Boolean,
    val query: String,
    val landing: Boolean,
    val mapExpanded: Boolean,
    val roastersExpanded: Boolean,
    val showRoasterResults: Boolean,
    val canSuggestRoaster: Boolean,
    val scrollRevision: Int,
    val bottomClearance: Float,
    val loading: Boolean,
    val refreshing: Boolean,
    val loadingMore: Boolean,
    val canLoadMore: Boolean,
    val failed: Boolean,
    val loadMoreFailed: Boolean,
    val canSuggestShop: Boolean,
    val emptyReason: String?,
    val locationNotice: String?,
    val settingsNeeded: Boolean,
    val favoriteFailure: Boolean,
    val continueCheckIn: Boolean,
    val shops: List<DiscoveryIosShop>,
    val recent: List<DiscoveryIosShop>,
    val mapShops: List<DiscoveryIosShop>,
    val selectedShopId: String?,
    val suggestions: List<DiscoveryIosShop>,
    val showSuggestions: Boolean,
    val searchFailed: Boolean,
    val searchLoading: Boolean,
    val zone: DiscoveryIosZone?,
    val zonesVisible: Boolean,
    val heatmapVisible: Boolean,
    val heatmapLoading: Boolean,
    val heatmapFailed: Boolean,
    val viewportFailed: Boolean,
    val truncated: Boolean,
    val chips: List<DiscoveryIosChip>,
    val activeFilterCount: Int,
    val labels: Map<String, String>,
    val pickerResultQuery: String?,
    val pickerResultNearby: Boolean,
    val allResultsEmpty: Boolean,
)

data class DiscoveryIosShop(
    val id: String, val title: String, val photoUrl: String?, val address: String?,
    val rating: String?, val detail: String?, val distance: String?, val kind: String,
    val tags: List<String>, val favorite: Boolean, val favoriteBusy: Boolean,
    val favoriteLabel: String, val open: Boolean, val isNew: Boolean, val hours: String?, val decoration: DiscoveryIosShopDecoration?,
)

data class DiscoveryIosChip(val id: String, val title: String, val selected: Boolean)
data class DiscoveryIosZone(val title: String, val description: String?, val count: Int)

data class DiscoveryIosShopDecoration(
    val priceLevel: Int, val ratingCount: Int, val brewMethods: List<DiscoveryIosBrewMethod>, val roasterLogos: List<String>,
)

data class DiscoveryIosBrewMethod(val name: String, val icon: platform.UIKit.UIImage?)
