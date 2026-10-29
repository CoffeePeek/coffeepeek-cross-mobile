package com.coffeepeek.admin.ui.screen.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.DpSize
import com.coffeepeek.admin.map.CoffeeMap
import com.coffeepeek.admin.map.mapCanvasSize
import com.coffeepeek.admin.map.zoneMapCluster
import com.coffeepeek.domain.model.MapShop

/** Native canvas adapter; screen previews inject a local surface instead. */
@Composable
internal fun MapCanvas(
    state: MapUiState,
    actions: MapScreenActions,
    pendingFocus: MapShop?,
    isPreview: Boolean,
    canvasSize: DpSize?,
    modifier: Modifier,
) {
    val shops = pendingFocus?.let { (state.shops + it).distinctBy { shop -> shop.id } } ?: state.shops
    CoffeeMap(
        shops = shops,
        clusters = state.clusters + if (isPreview || !state.showZones) state.zoneGroups.filter { it.shopCount > 0 }.map(::zoneMapCluster) else emptyList(),
        zones = if (!isPreview && state.showZones) state.zones else emptyList(),
        selectedShopId = state.selectedShop?.id ?: pendingFocus?.id,
        onBoundsChanged = actions.onBoundsChanged,
        onShopClick = actions.onShopClick,
        onZoneClick = actions.onZoneClick,
        modifier = modifier.mapCanvasSize(canvasSize).then(if (isPreview) Modifier.clearAndSetSemantics {} else Modifier),
        cameraTarget = state.cameraTarget ?: pendingFocus?.let { it.latitude to it.longitude },
        cameraZoom = state.cameraZoom ?: if (pendingFocus != null) 16f else null,
        onCameraTargetApplied = actions.onCameraApplied,
        isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
        myLocationRequestKey = state.myLocationRequest,
        onMyLocationFound = actions.onLocationFound,
        onLocationPermissionDenied = actions.onLocationDenied,
        requestLocationPermissionOnLoad = !isPreview,
        showAttribution = !isPreview,
    )
}
