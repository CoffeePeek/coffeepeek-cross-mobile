package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_collapse
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_my_location
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_search_placeholder
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_truncated
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zones_hide
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zones_show
import com.coffeepeek.feature.discovery.impl.resources.discovery_search_clear
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.component.CoffeePeekMapPreviewOverlay
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.discovery.api.MapCanvasCallbacks
import com.coffeepeek.feature.discovery.impl.map.canvasClusters
import com.coffeepeek.feature.discovery.api.MapCanvasState
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapEvent
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun MapScreen(
    viewModel: MapViewModel,
    host: DiscoveryHost,
    onOpenShop: (String) -> Unit,
    bottomClearance: Dp,
    focus: MapFocus?,
    onFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    /** The small map is a read-only preview; tapping it expands the map. */
    isPreview: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    /** The canvas keeps this size while its container animates, so the map is not resized (and redrawn) on every frame. */
    canvasSize: DpSize? = null,
    onOpenFilters: () -> Unit,
    onSearchAction: (SearchAction) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    host.locationPermission(
        state.locationPermissionRequest,
        { viewModel.onAction(MapAction.LocationGranted) },
        { viewModel.onAction(MapAction.LocationDenied) },
    )
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
    val callbacks = rememberMapCallbacks(viewModel, dismissInput)
    val toggleExpand = onToggleExpand?.let { toggle ->
        {
            viewModel.onAction(MapAction.Interact)
            toggle()
        }
    }
    val mapModifier = if (isPreview) modifier else modifier.mapInteraction { viewModel.onAction(MapAction.Interact) }
    MapContent(state, host, callbacks, bottomClearance, dismissInput, viewModel::onAction, mapModifier, isPreview, toggleExpand, canvasSize, onOpenFilters, onSearchAction)
}

@Composable
private fun rememberMapCallbacks(viewModel: MapViewModel, dismissInput: () -> Unit): MapCanvasCallbacks {
    val currentDismissInput by rememberUpdatedState(dismissInput)
    return remember(viewModel) {
        MapCanvasCallbacks(
            onBoundsChanged = { bounds, zoom -> viewModel.onAction(MapAction.BoundsChanged(bounds, zoom)) },
            // As in prod: a tap on a marker first closes the keyboard and the search field.
            onMarkerClick = { id -> currentDismissInput(); viewModel.onAction(MapAction.SelectMarker(id)) },
            onZoneClick = { id -> viewModel.onAction(MapAction.SelectZone(id)) },
            onCameraApplied = { viewModel.onAction(MapAction.CameraApplied) },
            onMyLocationFound = { point -> viewModel.onAction(MapAction.MyLocationFound(point)) },
            onLocationDenied = { viewModel.onAction(MapAction.LocationDenied) },
        )
    }
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
    onOpenFilters: () -> Unit,
    onSearchAction: (SearchAction) -> Unit,
) {
    Box(host.exposure(modifier.fillMaxSize(), if (isPreview) DiscoveryExposure.MapPreview else DiscoveryExposure.MapFull)) {
        host.mapCanvas(
            MapCanvasState(
                state.markers, if (state.showZones && !isPreview) state.zones else emptyList(), state.selectedShop?.id, state.camera, state.myLocationRequest, isPreview,
                canvasSize = canvasSize,
                clusters = state.canvasClusters(isPreview),
            ),
            canvasCallbacks,
            Modifier.align(Alignment.Center).then(if (isPreview) Modifier.clearAndSetSemantics {} else Modifier),
        )
        if (isPreview) {
            if (onToggleExpand != null) {
                CoffeePeekMapPreviewOverlay(onToggleExpand, Modifier.matchParentSize())
            }
            return@Box
        }
        MapSearchControls(state, dismissInput, onAction, onToggleExpand, onOpenFilters,
            Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth()
                .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3).zIndex(2f), onSearchAction)
        MapOverlayControls(state, host, bottomClearance, dismissInput, onAction)
    }
}

@Preview
@Composable
private fun MapContentPreview() = CoffeePeekTheme {
    MapContent(MapState(isTruncated = true), DiscoveryHost(), MapCanvasCallbacks({ _, _ -> }, {}, {}, {}, {}, {}),
        0.dp, {}, {}, onOpenFilters = {}, onSearchAction = {})
}
