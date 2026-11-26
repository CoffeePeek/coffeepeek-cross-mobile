package com.coffeepeek.admin.di.shop

/** Objective-C-exported display values keep feature implementation types out of the Swift contract. */
data class IosShopDetailData(
    val generation: Int, val loading: Boolean, val hasError: Boolean, val loggedIn: Boolean,
    val favorite: Boolean, val favoriteEnabled: Boolean, val favoriteBusy: Boolean,
    val favoriteSelection: Boolean, val favoriteLabel: String,
    val scheduleExpanded: Boolean, val featuresExpanded: Boolean, val priceInfoVisible: Boolean,
    val message: String?, val details: IosShopDetails?,
    val labels: Map<String, String>, val checkInDates: Map<String, String>,
)

data class IosShopDetails(
    val id: String, val slug: String, val title: String, val summary: String?, val address: String?,
    val distance: String?, val kind: String, val rating: String, val reviews: String, val isOpen: Boolean, val canRoute: Boolean,
    val photos: List<IosShopDetailPhoto>, val roasters: List<IosShopDetailRoaster>,
    val beans: List<String>, val equipment: List<String>, val scheduleStatus: String,
    val schedule: List<IosShopDetailSchedule>, val menu: IosShopDetailMenu?, val contacts: List<IosShopDetailContact>,
    val features: List<IosShopDetailFeature>, val featureToggle: String, val priceValue: String?, val priceTiers: List<IosShopDetailPriceTier>,
)

data class IosShopDetailFeature(val name: String, val icon: platform.UIKit.UIImage?)

data class IosShopDetailPhoto(val id: String, val url: String, val label: String)
data class IosShopDetailRoaster(val id: String, val name: String, val photo: String?, val label: String)
data class IosShopDetailSchedule(val day: String, val hours: String, val today: Boolean)
data class IosShopDetailMenu(val groups: List<List<IosShopDetailMenuItem>>, val photos: List<IosShopDetailPhoto>, val freshness: List<String>)
data class IosShopDetailMenuItem(val id: String, val title: String, val amount: String?, val currency: String, val priceLabel: String?)
data class IosShopDetailContact(val kind: String, val title: String, val target: String, val copyable: Boolean, val label: String)
data class IosShopDetailPriceTier(val count: Int, val label: String, val selected: Boolean)
