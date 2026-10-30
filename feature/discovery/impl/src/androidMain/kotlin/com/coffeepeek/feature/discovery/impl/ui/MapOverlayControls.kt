package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.MapShopDetails
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapMarker

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MapOverlayControls(
    state: MapState, host: DiscoveryHost, bottomClearance: Dp, dismissInput: () -> Unit, onAction: (MapAction) -> Unit,
) {
    var carouselHeightPx by remember { mutableIntStateOf(0) }
    val carouselHeight = with(LocalDensity.current) { carouselHeightPx.toDp() }
    MapPositionControls(state, bottomClearance, carouselHeight, onAction)
    MapSelection(state, host, bottomClearance, dismissInput, onAction) { carouselHeightPx = it }
}

@PreviewLightDark
@Composable
private fun MapOverlayControlsPreview() = CoffeePeekTheme {
    val shop = MapMarker("preview", "CoffeePeek Specialty Coffee", Coordinates(53.9, 27.56))
    val details = MapShopDetails(null, 4.5, 12, true, "09:00 – 21:00")
    val state = MapState(carouselShops = listOf(shop), cardDetails = mapOf(shop.id to details))
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(Modifier.fillMaxWidth().height(480.dp)) {
            MapOverlayControls(state, DiscoveryHost(), 0.dp, {}, {})
        }
    }
}


@Composable
private fun androidx.compose.foundation.layout.BoxScope.MapPositionControls(state: MapState, bottomClearance: Dp, carouselHeight: Dp, onAction: (MapAction) -> Unit) {
    MapZoomControl({ onAction(MapAction.ZoomIn) }, { onAction(MapAction.ZoomOut) }, Modifier.align(Alignment.CenterEnd).padding(end = CoffeePeekDimens.spacing4))
    val controlsBottom = bottomClearance + when {
        state.selectedZone != null -> 180.dp
        state.hasCarousel -> carouselHeight + CoffeePeekDimens.spacing4 * 2
        else -> CoffeePeekDimens.spacing4
    }
    Column(
        Modifier.align(Alignment.BottomEnd).padding(end = CoffeePeekDimens.spacing4, bottom = controlsBottom),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), horizontalAlignment = Alignment.End,
    ) {
        val zones = stringResource(if (state.showZones) R.string.discovery_map_zones_hide else R.string.discovery_map_zones_show)
        CoffeePeekGlassIconButton({ onAction(MapAction.ToggleZones) }, zones) {
            Icon(CoffeePeekIcons.Polygon, null, Modifier.size(26.dp), tint = if (state.showZones) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        CoffeePeekGlassIconButton({ onAction(MapAction.RequestMyLocation) }, stringResource(R.string.discovery_map_my_location), Modifier.size(CoffeePeekDimens.buttonHeight + 4.dp)) {
            Icon(CoffeePeekIcons.Navigation, null, Modifier.size(32.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.MapSelection(
    state: MapState, host: DiscoveryHost, bottomClearance: Dp, dismissInput: () -> Unit, onAction: (MapAction) -> Unit,
    onCarouselHeight: (Int) -> Unit,
) {
    val zone = state.selectedZone
    if (zone != null) {
        MapZoneCard(
            zone, { onAction(MapAction.ShowZoneShops) }, { onAction(MapAction.ClearZone) },
            Modifier.align(Alignment.BottomCenter).padding(start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4, bottom = bottomClearance + CoffeePeekDimens.spacing4),
        )
    } else if (state.hasCarousel) {
        MapShopCarousel(state, host, dismissInput, onAction, Modifier.align(Alignment.BottomCenter)
            .padding(bottom = bottomClearance + CoffeePeekDimens.spacing4).onSizeChanged { onCarouselHeight(it.height) })
    }
}
