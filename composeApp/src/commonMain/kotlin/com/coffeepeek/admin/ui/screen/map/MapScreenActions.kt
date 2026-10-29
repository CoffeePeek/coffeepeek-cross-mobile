package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop

/** Runtime callbacks shared by the map's stateless rendering components. */
internal data class MapScreenActions(
    val onBoundsChanged: (MapBounds, Float) -> Unit = { _, _ -> },
    val onShopClick: (MapShop) -> Unit = {},
    val onCarouselSelect: (MapShop) -> Unit = {},
    val onSearchSelect: (CoffeeShop) -> Unit = {},
    val onZoneClick: (MapCoffeeZone) -> Unit = {},
    val onCameraApplied: () -> Unit = {},
    val onLocationFound: (Double, Double) -> Unit = { _, _ -> },
    val onLocationDenied: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onExpand: () -> Unit = {},
    val onCollapse: (() -> Unit)? = null,
    val onZoomIn: () -> Unit = {},
    val onZoomOut: () -> Unit = {},
    val onToggleZones: () -> Unit = {},
    val onMyLocation: () -> Unit = {},
    val onShowZoneShops: () -> Unit = {},
    val onDismissZone: () -> Unit = {},
    val onOpenShop: (String) -> Unit = {},
)
