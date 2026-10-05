package com.coffeepeek.admin.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop

@Composable
expect fun CoffeeMap(
    shops: List<MapShop>,
    clusters: List<MapCluster> = emptyList(),
    zones: List<MapCoffeeZone> = emptyList(),
    selectedShopId: String?,
    onBoundsChanged: (MapBounds, Float) -> Unit,
    onShopClick: (MapShop) -> Unit,
    onZoneClick: (MapCoffeeZone) -> Unit = {},
    modifier: Modifier = Modifier,
    cameraTarget: Pair<Double, Double>? = null,
    cameraZoom: Float? = null,
    onCameraTargetApplied: () -> Unit = {},
    isDarkTheme: Boolean = false,
    myLocationRequestKey: Int = 0,
    onMyLocationFound: (Double, Double) -> Unit = { _, _ -> },
    onLocationPermissionDenied: () -> Unit = {},
    requestLocationPermissionOnLoad: Boolean = true,
)
