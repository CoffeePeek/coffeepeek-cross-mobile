package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import com.coffeepeek.feature.discovery.impl.tab.Frame
import com.coffeepeek.feature.discovery.impl.tab.previewFrame
import com.coffeepeek.feature.discovery.impl.search.SearchAction

internal const val MAP_ITEM_KEY = "mini-map"

/** Transient overlay/layout state; the ViewModels keep durable search and map state. */
@Stable
internal class DiscoveryTabState(
    val list: LazyListState,
    val shops: LazyListState,
    val results: LazyListState,
    roasterVisibility: MutableState<Boolean>,
) {
    var showRoasters by roasterVisibility
    var mapExpanded by mutableStateOf(false)
    var mapAnchor by mutableStateOf(Frame.Zero)
    var rootOrigin by mutableStateOf(Offset.Zero)
    var viewportTop by mutableFloatStateOf(0f)
    var viewportHeight by mutableFloatStateOf(0f)
    val previewBounds by derivedStateOf {
        val layout = list.layoutInfo
        previewFrame(mapAnchor, viewportTop, layout.visibleItemsInfo.firstOrNull { it.key == MAP_ITEM_KEY }?.offset,
            layout.beforeContentPadding)
    }

    fun closeRoasters(onAction: (SearchAction) -> Unit) {
        onAction(SearchAction.CancelSearch)
        showRoasters = false
    }

    fun recordMapSlot(slot: Frame, ready: Boolean, discoveryProgress: Float, expansion: Float) {
        if (ready && discoveryProgress == 1f && !mapExpanded && expansion == 0f) {
            mapAnchor = Frame.of(slot.left - rootOrigin.x, slot.top - rootOrigin.y, slot.width, slot.height)
        }
    }

    fun recordViewport(coordinates: LayoutCoordinates) {
        viewportTop = coordinates.positionInRoot().y - rootOrigin.y
        viewportHeight = coordinates.size.height.toFloat()
    }
}

@Composable
internal fun rememberDiscoveryTabState(): DiscoveryTabState {
    val roasters = rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val shops = rememberLazyListState()
    val results = rememberLazyListState()
    return remember(list, shops, results, roasters) { DiscoveryTabState(list, shops, results, roasters) }
}

@Stable
internal class DiscoveryTabMotion(
    val searchOpacity: State<Float>,
    val discoveryProgress: State<Float>,
    val expansion: State<Float>,
)

@Composable
internal fun discoveryTabMotion(ui: DiscoveryTabState, showDiscovery: Boolean, reduceMotion: Boolean): DiscoveryTabMotion {
    val search = animateFloatAsState(if (ui.showRoasters) 0f else 1f,
        tween(if (reduceMotion) 0 else 360), label = "discovery-roaster-list")
    val progress = animateFloatAsState(if (showDiscovery) 1f else 0f,
        tween(if (reduceMotion) 0 else 360), label = "discovery-map-slide")
    val expansion = animateFloatAsState(if (ui.mapExpanded) 1f else 0f,
        tween(if (reduceMotion) 0 else 420, easing = FastOutSlowInEasing), label = "discovery-map-expansion")
    return remember(search, progress, expansion) { DiscoveryTabMotion(search, progress, expansion) }
}
