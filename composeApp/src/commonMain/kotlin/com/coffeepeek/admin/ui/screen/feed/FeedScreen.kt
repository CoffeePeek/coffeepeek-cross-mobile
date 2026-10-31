package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.DpSize
import com.coffeepeek.admin.di.platformViewModel

@Composable
fun FeedScreen(
    onSelectRoasters: () -> Unit,
    vm: FeedViewModel = platformViewModel(),
    mapPreview: (@Composable (Boolean, () -> Unit, DpSize, Modifier) -> Unit)? = null,
    roasterPreview: (@Composable () -> Unit)? = null,
    onMapExpandedChange: (Boolean) -> Unit = {},
    hasRoasterResults: Boolean? = null,
    reselections: kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow(),
) {
    val state by vm.uiState.collectAsState()
    val layout = rememberFeedLayout()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissInput: () -> Unit = {
        focus.clearFocus(force = true)
        keyboard?.hide()
    }
    var locationRefreshKey by remember { mutableStateOf(0) }
    val location = FeedLocationEffects(vm, locationRefreshKey) { locationRefreshKey++ }
    val actions = feedScreenActions(vm, state, dismissInput, onSelectRoasters) { locationRefreshKey++ }
    FeedRuntimeEffects(layout, state, mapPreview != null, onMapExpandedChange, actions)
    FeedReselectionEffect(layout, state.showDiscovery, reselections, dismissInput, vm::closeFilters)
    FeedScreenContent(state, layout, actions, location, mapPreview, roasterPreview, hasRoasterResults)
}

internal fun expandedMapBounds(preview: Rect, screenSize: Size, expansion: Float): Rect =
    lerp(preview, Rect(Offset.Zero, screenSize), expansion.coerceIn(0f, 1f))

internal fun discoveryMapOffsetY(viewportHeight: Float, progress: Float): Float =
    viewportHeight * (progress.coerceIn(0f, 1f) - 1f)

// The measured anchor includes pull-to-refresh placement as well as list scrolling.
internal fun discoveryMapPreviewBounds(anchor: Rect, viewportTop: Float, itemOffset: Int?): Rect =
    if (itemOffset != null) anchor
    else Rect(Offset(anchor.left, viewportTop - anchor.height), anchor.size)
