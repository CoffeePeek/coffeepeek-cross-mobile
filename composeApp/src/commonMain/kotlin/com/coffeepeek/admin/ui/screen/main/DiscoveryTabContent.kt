package com.coffeepeek.admin.ui.screen.main

import com.coffeepeek.admin.analytics.*

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.screen.feed.FeedScreen
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.ui.screen.feed.DiscoveryMapFilterBar
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.ui.screen.map.MapScreen
import com.coffeepeek.admin.ui.screen.map.MapViewModel
import com.coffeepeek.admin.ui.screen.map.DiscoveryMapFiltersButton
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import org.koin.compose.koinInject

/** The same discovery content is hosted by Compose navigation and native iOS tabs. */
@Composable
internal fun DiscoveryTabContent(onMapExpandedChange: (Boolean) -> Unit) {
    val reduceMotion = com.coffeepeek.admin.ui.component.platformReduceMotion()
    var showRoasters by rememberSaveable { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }
    val feedVm: FeedViewModel = platformViewModel()
    val feedState by feedVm.uiState.collectAsState()
    val mapVm: MapViewModel = platformViewModel()
    LaunchedEffect(feedState.query, feedState.filters, feedState.searchOrigin) {
        mapVm.syncSearch(feedState.query, feedState.filters, feedState.searchOrigin)
    }
    val roasters = koinInject<RoasterScreenRenderer>()
    val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
    var previousOverlay by remember { mutableStateOf("discovery") }
    val overlay = if (showRoasters) "roasters" else if (isMapExpanded) "map" else "discovery"
    LaunchedEffect(overlay) {
        if (overlay != previousOverlay) {
            Analytics.service?.screenShown(overlay, correlationId("screen"))
            previousOverlay = overlay
        }
    }
    LaunchedEffect(pendingMapFocus) {
        if (pendingMapFocus != null) showRoasters = false
    }
    val searchOpacity = animateFloatAsState(if (showRoasters) 0f else 1f, tween(if (reduceMotion) 0 else 360), label = "discovery-roaster-list")
    val searchVisible by remember { derivedStateOf { searchOpacity.value > 0f } }
    CheckInTabContent(showCreateButton = !isMapExpanded) {
        RetainedContent(visible = searchVisible) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = searchOpacity.value
                translationY = -size.height / 5f * (1f - searchOpacity.value)
            }) {
                FeedScreen(
                    vm = feedVm,
                    onSelectRoasters = { showRoasters = true },
                    mapPreview = { expanded, onToggleExpand, canvasSize, modifier ->
                        MapScreen(
                            vm = mapVm,
                            modifier = modifier,
                            isPreview = !expanded,
                            onToggleExpand = onToggleExpand,
                            canvasSize = canvasSize,
                            onQueryChange = feedVm::onQueryChange,
                            filterControl = { DiscoveryMapFiltersButton(feedState.activeFilterCount, feedVm::toggleFilters) },
                            filterBar = { DiscoveryMapFilterBar(feedState, feedVm) },
                        )
                    },
                    roasterPreview = {
                        roasters.DiscoverySection(
                            query = feedState.query,
                            selectedRoasterIds = feedState.filters.roasterIds,
                            favoritesOnly = feedState.filters.favoritesOnly,
                        )
                    },
                    hasRoasterResults = if (feedState.query.isNotBlank()) roasters.hasDiscoveryResults(
                        feedState.query, feedState.filters.roasterIds, feedState.filters.favoritesOnly,
                    ) else null,
                    onMapExpandedChange = {
                        isMapExpanded = it
                        onMapExpandedChange(it)
                    },
                )
            }
        }
        AnimatedVisibility(
            visible = showRoasters,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(if (reduceMotion) 0 else 300)) + slideInVertically(tween(if (reduceMotion) 0 else 360)) { it / 5 },
            exit = fadeOut(tween(if (reduceMotion) 0 else 240)) + slideOutVertically(tween(if (reduceMotion) 0 else 360)) { -it / 5 },
        ) {
            roasters.DiscoverySearch(
                onCancel = {
                    feedVm.cancelSearch()
                    showRoasters = false
                },
                query = feedState.query,
                onQueryChange = feedVm::onQueryChange,
                selectedRoasterIds = feedState.filters.roasterIds,
                favoritesOnly = feedState.filters.favoritesOnly,
            )
        }
    }
}
