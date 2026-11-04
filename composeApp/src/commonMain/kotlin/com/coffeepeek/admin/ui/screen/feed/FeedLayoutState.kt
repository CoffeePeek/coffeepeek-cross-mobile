package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

@Stable
internal class FeedLayoutState(val listState: LazyListState, val discoveryListState: LazyListState, val discoveryShopsState: LazyListState) {
    var mapExpanded by mutableStateOf(false)
    var mapAnchor by mutableStateOf(Rect.Zero)
    var rootOrigin by mutableStateOf(Offset.Zero)
    var viewportTop by mutableStateOf(0f)
    var viewportHeight by mutableStateOf(0f)
    val previewBounds: Rect
        get() = discoveryMapPreviewBounds(mapAnchor, viewportTop,
            discoveryListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "mini-map" }?.offset)
}

@Composable
internal fun rememberFeedLayout(): FeedLayoutState {
    val list = rememberLazyListState()
    val discovery = rememberLazyListState()
    val shops = rememberLazyListState()
    return remember { FeedLayoutState(list, discovery, shops) }
}

internal data class FeedMapMotion(val progress: State<Float>, val expansion: State<Float>)
