package com.coffeepeek.admin.ui.screen.main

import com.coffeepeek.admin.analytics.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.component.platformReduceMotion
import com.coffeepeek.admin.ui.screen.feed.DiscoveryMapFilterBar
import com.coffeepeek.admin.ui.screen.feed.FeedScreen
import com.coffeepeek.admin.ui.screen.feed.FeedUiState
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.ui.screen.map.DiscoveryMapFiltersButton
import com.coffeepeek.admin.ui.screen.map.MapScreen
import com.coffeepeek.admin.ui.screen.map.MapViewModel
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

/** The same discovery content is hosted by Compose navigation and native iOS tabs. */
@Composable
internal fun DiscoveryTabContent(onMapExpandedChange: (Boolean) -> Unit) {
    val reduceMotion = platformReduceMotion()
    var showRoasters by rememberSaveable { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }
    val feedVm: FeedViewModel = platformViewModel()
    val feedState by feedVm.uiState.collectAsState()
    val mapVm: MapViewModel = platformViewModel()
    val roasters = koinInject<RoasterScreenRenderer>()
    DiscoveryTabEffects(feedState, feedVm, mapVm, showRoasters, isMapExpanded) { showRoasters = false }
    CheckInTabContent(showCreateButton = !isMapExpanded) {
        DiscoveryLayers(
            showRoasters = showRoasters,
            reduceMotion = reduceMotion,
            feedContent = {
                DiscoveryFeedContent(feedState, feedVm, mapVm, roasters, showRoasters,
                    onSelectRoasters = { showRoasters = true },
                    onMapExpandedChange = { expanded ->
                        isMapExpanded = expanded
                        onMapExpandedChange(expanded)
                    })
            },
            roasterContent = {
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
            },
        )
    }
}

@Composable
private fun DiscoveryTabEffects(
    state: FeedUiState,
    feedVm: FeedViewModel,
    mapVm: MapViewModel,
    showRoasters: Boolean,
    isMapExpanded: Boolean,
    onMapFocus: () -> Unit,
) {
    LaunchedEffect(state.query, state.filters, state.searchOrigin) {
        mapVm.syncSearch(state.query, state.filters, state.searchOrigin)
    }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val listActive = LocalAnalyticsVisible.current && lifecycle == Lifecycle.State.RESUMED && !showRoasters && !isMapExpanded
    DisposableEffect(feedVm, listActive) {
        feedVm.setSearchAnalyticsActive(listActive)
        onDispose { feedVm.setSearchAnalyticsActive(false) }
    }
    var previousOverlay by remember { mutableStateOf("discovery") }
    val overlay = if (showRoasters) "roasters" else if (isMapExpanded) "map" else "discovery"
    LaunchedEffect(overlay) {
        if (overlay != previousOverlay) {
            Analytics.service?.screenShown(overlay, correlationId("screen"))
            previousOverlay = overlay
        }
    }
    val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
    LaunchedEffect(pendingMapFocus) {
        if (pendingMapFocus != null) onMapFocus()
    }
}

@Composable
private fun DiscoveryFeedContent(
    state: FeedUiState,
    feedVm: FeedViewModel,
    mapVm: MapViewModel,
    roasters: RoasterScreenRenderer,
    showRoasters: Boolean,
    onSelectRoasters: () -> Unit,
    onMapExpandedChange: (Boolean) -> Unit,
) {
    FeedScreen(
        vm = feedVm,
        onSelectRoasters = onSelectRoasters,
        mapPreview = { expanded, onToggleExpand, canvasSize, modifier ->
            MapScreen(
                vm = mapVm,
                modifier = modifier,
                isPreview = !expanded,
                isSearchActive = expanded && !showRoasters,
                onToggleExpand = onToggleExpand,
                canvasSize = canvasSize,
                onQueryChange = { query ->
                    mapVm.onQueryChange(query)
                    feedVm.onQueryChange(query)
                },
                filterControl = { DiscoveryMapFiltersButton(state.activeFilterCount, feedVm::toggleFilters) },
                filterBar = { DiscoveryMapFilterBar(state, feedVm) },
            )
        },
        roasterPreview = {
            roasters.DiscoverySection(state.query, state.filters.roasterIds, state.filters.favoritesOnly)
        },
        hasRoasterResults = if (state.query.isNotBlank()) roasters.hasDiscoveryResults(
            state.query, state.filters.roasterIds, state.filters.favoritesOnly,
        ) else null,
        onMapExpandedChange = onMapExpandedChange,
    )
}

@Composable
private fun DiscoveryLayers(
    showRoasters: Boolean,
    reduceMotion: Boolean,
    feedContent: @Composable () -> Unit,
    roasterContent: @Composable () -> Unit,
) {
    val opacity = animateFloatAsState(if (showRoasters) 0f else 1f,
        tween(if (reduceMotion) 0 else 360), label = "discovery-roaster-list")
    val searchVisible by remember { derivedStateOf { opacity.value > 0f } }
    RetainedContent(visible = searchVisible) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = opacity.value
            translationY = -size.height / 5f * (1f - opacity.value)
        }) { feedContent() }
    }
    AnimatedVisibility(
        visible = showRoasters,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(if (reduceMotion) 0 else 300)) + slideInVertically(tween(if (reduceMotion) 0 else 360)) { it / 5 },
        exit = fadeOut(tween(if (reduceMotion) 0 else 240)) + slideOutVertically(tween(if (reduceMotion) 0 else 360)) { -it / 5 },
    ) { roasterContent() }
}

@Composable
private fun DiscoveryLayersPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.fillMaxSize()) {
        DiscoveryLayers(false, true,
            feedContent = { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {} },
            roasterContent = { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {} })
    }
}

@Preview
@Composable
private fun DiscoveryLayersLightPreview() = DiscoveryLayersPreview(false)

@Preview
@Composable
private fun DiscoveryLayersDarkPreview() = DiscoveryLayersPreview(true)
