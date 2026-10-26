package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.MapCanvasCallbacks
import com.coffeepeek.feature.discovery.impl.map.canvasClusters
import com.coffeepeek.feature.discovery.api.MapCanvasState
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapEvent
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.MapViewModel

@Composable
internal fun MapScreen(
    viewModel: MapViewModel,
    host: DiscoveryHost,
    onOpenShop: (String) -> Unit,
    bottomClearance: Dp,
    focus: MapFocus?,
    onFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    /** The small map of the tab: a tap on a marker opens the shop, and only the expand, zoom and location buttons are drawn. */
    isPreview: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    /** The canvas keeps this size while its container animates, so the map is not resized (and redrawn) on every frame. */
    canvasSize: DpSize? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissInput: () -> Unit = { focusManager.clearFocus(force = true); keyboard?.hide() }
    LaunchedEffect(viewModel) { viewModel.events.collect { event -> if (event is MapEvent.OpenShop) onOpenShop(event.shopId) } }
    LaunchedEffect(focus) {
        if (focus != null) {
            dismissInput()
            viewModel.onAction(MapAction.Focus(focus))
            onFocusConsumed()
        }
    }
    val currentDismissInput by rememberUpdatedState(dismissInput)
    val callbacks = remember(viewModel, isPreview) {
        MapCanvasCallbacks(
            onBoundsChanged = { bounds, zoom -> viewModel.onAction(MapAction.BoundsChanged(bounds, zoom)) },
            // As in prod: a tap on a marker first closes the keyboard and the search field.
            onMarkerClick = { id -> currentDismissInput(); viewModel.onAction(if (isPreview) MapAction.OpenShop(id) else MapAction.SelectMarker(id)) },
            onZoneClick = { id -> viewModel.onAction(MapAction.SelectZone(id)) },
            onCameraApplied = { viewModel.onAction(MapAction.CameraApplied) },
            onMyLocationFound = { point -> viewModel.onAction(MapAction.MyLocationFound(point)) },
            onLocationDenied = { viewModel.onAction(MapAction.LocationDenied) },
        )
    }
    MapContent(state, host, callbacks, bottomClearance, dismissInput, viewModel::onAction, modifier, isPreview, onToggleExpand, canvasSize)
}

@Composable
internal fun MapContent(
    state: MapState,
    host: DiscoveryHost,
    canvasCallbacks: MapCanvasCallbacks,
    bottomClearance: Dp,
    dismissInput: () -> Unit,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    canvasSize: DpSize? = null,
) {
    Box(host.exposure(modifier.fillMaxSize(), if (isPreview) DiscoveryExposure.MapPreview else DiscoveryExposure.MapFull)) {
        host.mapCanvas(
            MapCanvasState(
                state.markers, if (state.showZones && !isPreview) state.zones else emptyList(), state.selectedShop?.id, state.camera, state.myLocationRequest, isPreview,
                clusters = state.canvasClusters(isPreview),
            ),
            canvasCallbacks,
            Modifier.align(Alignment.Center).then(canvasSize?.let { Modifier.requiredSize(it.width, it.height) } ?: Modifier.fillMaxSize()),
        )
        if (isPreview) {
            if (onToggleExpand != null) {
                CoffeePeekGlassIconButton(
                    onToggleExpand, stringResource(R.string.discovery_map_expand), Modifier.align(Alignment.TopEnd).padding(CoffeePeekDimens.spacing2), hazeState = null,
                ) { Icon(CoffeePeekIcons.Expand, null, Modifier.size(22.dp)) }
            }
            MapZoomControl({ onAction(MapAction.ZoomIn) }, { onAction(MapAction.ZoomOut) }, Modifier.align(Alignment.CenterEnd).padding(end = CoffeePeekDimens.spacing2))
            CoffeePeekGlassIconButton(
                { onAction(MapAction.RequestMyLocation) }, stringResource(R.string.discovery_map_my_location),
                Modifier.align(Alignment.BottomEnd).padding(end = CoffeePeekDimens.spacing2, bottom = CoffeePeekDimens.spacing2), hazeState = null,
            ) { Icon(CoffeePeekIcons.Navigation, null, Modifier.size(24.dp)) }
            return@Box
        }
        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth()
                .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3).zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                if (onToggleExpand != null) {
                    CoffeePeekGlassIconButton({ dismissInput(); onToggleExpand() }, stringResource(R.string.discovery_map_collapse), hazeState = null) {
                        Icon(CoffeePeekIcons.Close, null, Modifier.size(22.dp))
                    }
                }
                CoffeePeekSearchField(
                    value = state.query,
                    onValueChange = { onAction(MapAction.Query(it)) },
                    placeholder = stringResource(R.string.discovery_map_search_placeholder),
                    clearDescription = stringResource(R.string.discovery_search_clear),
                    fieldHeight = CoffeePeekDimens.buttonHeight,
                    onSearch = dismissInput,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.showSuggestions) MapSuggestions(state, onSelect = { dismissInput(); onAction(MapAction.SelectSuggestion(it)) })
        }
        if (state.isTruncated) {
            Surface(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp, start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4),
                shape = RoundedCornerShape(CoffeePeekDimens.radiusMd), color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, shadowElevation = 3.dp,
            ) {
                Text(
                    stringResource(R.string.discovery_map_truncated), style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing2),
                )
            }
        }
        MapZoomControl({ onAction(MapAction.ZoomIn) }, { onAction(MapAction.ZoomOut) }, Modifier.align(Alignment.CenterEnd).padding(end = CoffeePeekDimens.spacing4))
        val controlsBottom = bottomClearance + when {
            state.selectedZone != null -> 180.dp
            state.hasCarousel -> 132.dp
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
            // Prod draws the location button 4 dp larger than the zones button, with a larger arrow.
            CoffeePeekGlassIconButton({ onAction(MapAction.RequestMyLocation) }, stringResource(R.string.discovery_map_my_location), Modifier.size(CoffeePeekDimens.buttonHeight + 4.dp)) {
                Icon(CoffeePeekIcons.Navigation, null, Modifier.size(32.dp))
            }
        }
        if (state.showSearchArea) {
            Button(
                onClick = { onAction(MapAction.SearchArea) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomClearance + if (state.hasCarousel) 148.dp else 32.dp).height(CoffeePeekDimens.buttonHeight),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = CoffeePeekDimens.spacing5),
            ) { Text(stringResource(R.string.discovery_map_search_area), style = MaterialTheme.typography.labelLarge) }
        }
        val zone = state.selectedZone
        if (zone != null) {
            MapZoneCard(
                zone, { onAction(MapAction.ShowZoneShops) }, { onAction(MapAction.ClearZone) },
                Modifier.align(Alignment.BottomCenter).padding(start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4, bottom = bottomClearance + CoffeePeekDimens.spacing4),
            )
        } else if (state.hasCarousel) {
            MapShopCarousel(state, host, dismissInput, onAction, Modifier.align(Alignment.BottomCenter).padding(bottom = bottomClearance + CoffeePeekDimens.spacing4))
        }
    }
}
