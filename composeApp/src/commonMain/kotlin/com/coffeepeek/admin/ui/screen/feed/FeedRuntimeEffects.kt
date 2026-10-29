package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.LocationPermissionEffect
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.PrefetchListPhotos

@Composable
internal fun FeedLocationEffects(vm: FeedViewModel, refreshKey: Int, onGranted: () -> Unit): GeoPoint? {
    LocationPermissionEffect(requestKey = 1, onGranted = onGranted,
        onDenied = { trackDiscoveryLocation("unavailable") })
    val location = rememberPermittedUserLocation(refreshKey)
    LaunchedEffect(location) {
        location?.let {
            vm.onUserLocationChanged(it)
            trackDiscoveryLocation("available")
        }
    }
    return location
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FeedRuntimeEffects(
    layout: FeedLayoutState,
    state: FeedUiState,
    hasMap: Boolean,
    onExpanded: (Boolean) -> Unit,
    actions: FeedScreenActions,
) {
    val pendingFocus by Navigator.pendingMapFocus.collectAsState()
    LaunchedEffect(pendingFocus, layout.mapAnchor.width > 0f) {
        if (hasMap && pendingFocus != null) layout.mapExpanded = true
    }
    LaunchedEffect(layout.mapExpanded) { onExpanded(layout.mapExpanded) }
    DisposableEffect(Unit) { onDispose { onExpanded(false) } }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    BackHandler(lifecycle.isAtLeast(Lifecycle.State.STARTED) && (layout.mapExpanded || hasMap && !state.showDiscovery)) {
        if (layout.mapExpanded) layout.mapExpanded = false else actions.onCancelSearch()
    }
    val showDiscovery = hasMap && state.showDiscovery
    FeedPaginationEffect(layout.listState, state, showDiscovery, actions.onLoadMore)
    if (!layout.mapExpanded) {
        if (showDiscovery) PrefetchListPhotos(layout.discoveryShopsState, state.visibleShops.take(8).map { it.id to listOfNotNull(it.photoUrl) })
        else PrefetchListPhotos(layout.listState, state.visibleShops.map { it.id to listOfNotNull(it.photoUrl) })
    }
}

@Composable
private fun FeedPaginationEffect(listState: LazyListState, state: FeedUiState, showDiscovery: Boolean, loadMore: () -> Unit) {
    val shouldLoadMore by remember(showDiscovery, state) {
        derivedStateOf {
            if (showDiscovery || state.shops.isEmpty()) false else
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.shops.size - 5 &&
                    state.hasMore && !state.isLoadingMore && !state.isLoading && !state.isRefreshing
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) loadMore() }
}

private fun trackDiscoveryLocation(outcome: String) {
    Analytics.track(AnalyticsEvent.GEOLOCATION_RESULT, "placement" to "discovery", "trigger" to "automatic", "outcome" to outcome)
}
