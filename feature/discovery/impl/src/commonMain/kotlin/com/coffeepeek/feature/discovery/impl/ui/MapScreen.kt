package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_load_failed
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_retry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekMapPreviewOverlay
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.discovery.api.MapCanvasCallbacks
import com.coffeepeek.feature.discovery.impl.map.canvasClusters
import com.coffeepeek.feature.discovery.impl.map.canvasHeatmap
import com.coffeepeek.feature.discovery.api.MapCanvasState
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapEvent
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchAction

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
    onOpenFilters: (() -> Unit)?,
    onSearchAction: (SearchAction) -> Unit,
    showSearchField: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    host.locationPermission(
        state.locationPermissionRequest,
        { viewModel.onAction(MapAction.LocationGranted) },
        { viewModel.onAction(MapAction.LocationDenied) },
        { viewModel.onAction(MapAction.LocationPermanentlyDenied) },
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
    MapContent(state, host, callbacks, bottomClearance, dismissInput, viewModel::onAction, mapModifier,
        isPreview, toggleExpand, canvasSize, onOpenFilters, onSearchAction, showSearchField)
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
    onOpenFilters: (() -> Unit)?,
    onSearchAction: (SearchAction) -> Unit,
    showSearchField: Boolean = true,
) {
    Box(host.exposure(modifier.fillMaxSize(), if (isPreview) DiscoveryExposure.MapPreview else DiscoveryExposure.MapFull)) {
        host.mapCanvas(
            MapCanvasState(
                state.markers, if (state.showZones && !isPreview) state.zones else emptyList(), state.selectedShop?.id, state.camera, state.myLocationRequest, isPreview,
                canvasSize = canvasSize,
                clusters = state.canvasClusters(isPreview),
                heatmap = state.canvasHeatmap(isPreview),
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
                .padding(vertical = CoffeePeekDimens.spacing3).zIndex(2f), onSearchAction, showSearchField)
        if (state.viewportFailed && state.markers.isEmpty() && !state.locationSettingsNeeded) {
            MapViewportError({ onAction(MapAction.RetryViewport) },
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp).zIndex(2f))
        }
        if (state.locationSettingsNeeded) {
            DiscoveryLocationNotice(false, { onAction(MapAction.DismissLocationSettings) },
                onOpenSettings = host.openLocationSettings,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp).zIndex(2f))
        }
        MapOverlayControls(state, host, bottomClearance, dismissInput, onAction)
    }
}

@Preview
@Composable
private fun MapContentPreview() = MapContentPreviewContent(false)

@Preview @Composable private fun MapContentDarkPreview() = MapContentPreviewContent(true)

@Composable
private fun MapViewportError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekContentCard(modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.discovery_map_load_failed), Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text(stringResource(Res.string.discovery_map_retry))
            }
        }
    }
}

@Composable
private fun MapContentPreviewContent(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    MapContent(MapState(isTruncated = true), DiscoveryHost(), MapCanvasCallbacks({ _, _ -> }, {}, {}, {}, {}, {}),
        0.dp, {}, {}, onOpenFilters = {}, onSearchAction = {})
}
