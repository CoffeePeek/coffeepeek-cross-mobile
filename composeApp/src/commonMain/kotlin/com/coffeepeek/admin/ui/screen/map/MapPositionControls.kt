package com.coffeepeek.admin.ui.screen.map

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformMapControlButton
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BoxScope.MapPositionControls(
    state: MapUiState,
    actions: MapScreenActions,
    hasCarousel: Boolean,
    carouselHeight: Dp,
    clearance: Dp,
) {
    MapZoomControl(actions.onZoomIn, actions.onZoomOut,
        Modifier.align(Alignment.CenterEnd).padding(end = CpDimens.spacing4))
    val bottom = clearance + when {
        state.selectedZone != null -> 180.dp
        hasCarousel -> carouselHeight + CpDimens.spacing4 * 2
        else -> CpDimens.spacing4
    }
    MapLocationControls(state.showZones, actions, Modifier.align(Alignment.BottomEnd)
        .padding(end = CpDimens.spacing4, bottom = bottom))
}

@Composable
private fun MapLocationControls(showZones: Boolean, actions: MapScreenActions, modifier: Modifier) {
    val localizedLegacyMapPositionControlsDiscoveryMapZonesHide = stringResource(Res.string.legacy_map_position_controls_discovery_map_zones_hide)
    val localizedLegacyMapPositionControlsDiscoveryMapZonesShow = stringResource(Res.string.legacy_map_position_controls_discovery_map_zones_show)
    val localizedLegacyPickShopLocationDialogDiscoveryMapMyLocation = stringResource(Res.string.legacy_pick_shop_location_dialog_discovery_map_my_location)

    val zonesLabel = if (showZones) localizedLegacyMapPositionControlsDiscoveryMapZonesHide else localizedLegacyMapPositionControlsDiscoveryMapZonesShow
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2), horizontalAlignment = Alignment.End) {
        MapControlButton(GlassControlIcon.Zones, actions.onToggleZones, zonesLabel, selected = showZones,
            contentColor = if (showZones) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) {
            Icon(CpIcons.Polygon, zonesLabel, Modifier.size(26.dp))
        }
        MapControlButton(GlassControlIcon.Location, actions.onMyLocation, localizedLegacyPickShopLocationDialogDiscoveryMapMyLocation,
            Modifier.size(CpDimens.buttonHeight + 4.dp)) {
            Icon(CpIcons.Navigation, localizedLegacyPickShopLocationDialogDiscoveryMapMyLocation, Modifier.size(32.dp))
        }
    }
}

@Composable
private fun MapControlButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    PlatformMapControlButton(icon, onClick, contentDescription, modifier, selected) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@Composable
private fun MapZoomControl(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    val localizedLegacyMapPositionControlsDiscoveryMapZoomIn = stringResource(Res.string.legacy_map_position_controls_discovery_map_zoom_in)
    val localizedLegacyMapPositionControlsDiscoveryMapZoomOut = stringResource(Res.string.legacy_map_position_controls_discovery_map_zoom_out)

    val shape = RoundedCornerShape(CpDimens.radiusLg)
    Column(modifier.width(CpDimens.buttonHeight).clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
        MapZoomButton(CpIcons.Add, localizedLegacyMapPositionControlsDiscoveryMapZoomIn, onZoomIn,
            RoundedCornerShape(topStart = CpDimens.radiusLg, topEnd = CpDimens.radiusLg))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        MapZoomButton(CpIcons.Minus, localizedLegacyMapPositionControlsDiscoveryMapZoomOut, onZoomOut,
            RoundedCornerShape(bottomStart = CpDimens.radiusLg, bottomEnd = CpDimens.radiusLg))
    }
}

@Composable
private fun MapZoomButton(icon: ImageVector, label: String, onClick: () -> Unit, shape: RoundedCornerShape) {
    Box(Modifier.fillMaxWidth().height(CpDimens.buttonHeight).clip(shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MapPositionControlsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.size(390.dp, 600.dp)) {
        MapPositionControls(MapUiState(), MapScreenActions(), true, 140.dp, 24.dp)
    }
}

@Preview @Composable private fun MapPositionControlsLightPreview() = MapPositionControlsPreview(false)
@Preview @Composable private fun MapPositionControlsDarkPreview() = MapPositionControlsPreview(true)
