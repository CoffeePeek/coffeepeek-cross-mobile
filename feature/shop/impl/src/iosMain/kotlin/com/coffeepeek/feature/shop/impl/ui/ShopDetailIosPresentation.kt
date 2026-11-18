package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.impl.ShopCheckInListSnapshot

class ShopDetailIosPresentation internal constructor(
    val state: ShopDetailIosState,
    val onAction: (String, String, Int) -> Unit,
    val onDismissMessage: () -> Unit,
    val onOwnerEdit: () -> Unit,
    val languageTag: String,
)

data class ShopDetailIosState(
    val generation: Int, val loading: Boolean, val hasError: Boolean, val loggedIn: Boolean,
    val favorite: Boolean, val favoriteEnabled: Boolean, val favoriteBusy: Boolean,
    val favoriteSelection: Boolean, val favoriteLabel: String,
    val scheduleExpanded: Boolean, val featuresExpanded: Boolean, val priceInfoVisible: Boolean,
    val message: String?, val details: ShopDetailIosDetails?, val checkIns: ShopCheckInListSnapshot,
    val labels: Map<String, String>, val checkInDates: Map<String, String>,
)

data class ShopDetailIosDetails(
    val id: String, val slug: String, val title: String, val description: String?, val address: String?,
    val distance: String?, val kind: String, val rating: String, val reviews: String, val isOpen: Boolean, val canRoute: Boolean,
    val photos: List<ShopDetailIosPhoto>, val roasters: List<ShopDetailIosRoaster>,
    val beans: List<String>, val equipment: List<String>, val scheduleStatus: String,
    val schedule: List<ShopDetailIosSchedule>, val menu: ShopDetailIosMenu?, val contacts: List<ShopDetailIosContact>,
    val features: List<String>, val featureToggle: String, val priceValue: String?, val priceTiers: List<ShopDetailIosPriceTier>,
)

data class ShopDetailIosPhoto(val id: String, val url: String, val label: String)
data class ShopDetailIosRoaster(val id: String, val name: String, val photo: String?, val label: String)
data class ShopDetailIosSchedule(val day: String, val hours: String, val today: Boolean)
data class ShopDetailIosMenu(val groups: List<List<ShopDetailIosMenuItem>>, val photos: List<ShopDetailIosPhoto>, val freshness: List<String>)
data class ShopDetailIosMenuItem(val id: String, val title: String, val amount: String?, val currency: String, val priceLabel: String?)
data class ShopDetailIosContact(val kind: String, val title: String, val target: String, val copyable: Boolean, val label: String)
data class ShopDetailIosPriceTier(val count: Int, val label: String, val selected: Boolean)
