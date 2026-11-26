package com.coffeepeek.admin.di.discovery

/** App-owned, Objective-C-exported rendering values; feature implementation types remain hidden. */
data class IosDiscoveryState(
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
    val shops: List<IosDiscoveryShop>,
    val recent: List<IosDiscoveryShop>,
    val mapShops: List<IosDiscoveryShop>,
    val selectedShopId: String?,
    val suggestions: List<IosDiscoveryShop>,
    val showSuggestions: Boolean,
    val searchFailed: Boolean,
    val searchLoading: Boolean,
    val selectedZone: IosDiscoveryZone?,
    val zonesVisible: Boolean,
    val heatmapVisible: Boolean,
    val heatmapLoading: Boolean,
    val heatmapFailed: Boolean,
    val viewportFailed: Boolean,
    val truncated: Boolean,
    val chips: List<IosDiscoveryChip>,
    val activeFilterCount: Int,
    val labels: Map<String, String>,
    val pickerResultQuery: String?,
    val pickerResultNearby: Boolean,
    val allResultsEmpty: Boolean,
)

data class IosDiscoveryShop(
    val id: String, val title: String, val photoUrl: String?, val address: String?,
    val rating: String?, val detail: String?, val distance: String?, val kind: String,
    val tags: List<String>, val favorite: Boolean, val favoriteBusy: Boolean,
    val favoriteLabel: String, val open: Boolean, val isNew: Boolean, val hours: String?, val decoration: IosDiscoveryShopDecoration?,
)

data class IosDiscoveryChip(val id: String, val title: String, val selected: Boolean)
data class IosDiscoveryZone(val title: String, val summary: String?, val count: Int)

data class IosDiscoveryShopDecoration(
    val priceLevel: Int, val ratingCount: Int, val brewMethods: List<IosDiscoveryBrewMethod>, val roasterLogos: List<String>,
)

data class IosDiscoveryBrewMethod(val name: String, val icon: platform.UIKit.UIImage?)
