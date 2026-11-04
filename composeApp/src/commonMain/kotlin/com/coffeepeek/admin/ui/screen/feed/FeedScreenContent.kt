package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FeedScreenContent(state: FeedUiState, layout: FeedLayoutState, actions: FeedScreenActions,
    location: GeoPoint?, mapPreview: (@Composable (Boolean, () -> Unit, DpSize, Modifier) -> Unit)?,
    roasters: (@Composable () -> Unit)?, hasRoasterResults: Boolean?) {
    val showDiscovery = mapPreview != null && state.showDiscovery
    val showRoasters = roasters != null && state.query.isNotBlank()
    val shopsFirst = showRoasters && state.showsShopsFirst(hasRoasterResults)
    LaunchedEffect(showRoasters, shopsFirst) { layout.listState.scrollToItem(0) }
    val transition = updateTransition(showDiscovery, label = "discovery-shop-list")
    val progress = animateFloatAsState(if (showDiscovery || layout.mapExpanded) 1f else 0f, tween(360), label = "discovery-map-slide")
    val expansion = animateFloatAsState(if (layout.mapExpanded) 1f else 0f, tween(420, easing = FastOutSlowInEasing), label = "discovery-map-expansion")
    val motion = remember(progress, expansion) { FeedMapMotion(progress, expansion) }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { layout.rootOrigin = it.positionInRoot() }) {
        val screenSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        FeedScaffold(state, layout, actions, motion, mapPreview != null, location != null) { padding ->
            FeedListTransition(state, layout, actions, motion, transition, padding, location, screenSize, roasters, shopsFirst)
        }
        if (mapPreview != null) FeedMapLayer(layout, motion, screenSize, DpSize(maxWidth, maxHeight), mapPreview)
    }
}

@Composable
private fun FeedScaffold(state: FeedUiState, layout: FeedLayoutState, actions: FeedScreenActions,
    motion: FeedMapMotion, hasMap: Boolean, showNearby: Boolean, content: @Composable (PaddingValues) -> Unit) {
    CoffeePeekScaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { FeedHeader(state, layout, actions, motion, hasMap, showNearby) },
        containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (state.showFilters) ShopFiltersScreen(state, actions.onDismissFilters, actions.onApplyFilters)
        content(padding)
    }
}

@Composable
private fun FeedListTransition(state: FeedUiState, layout: FeedLayoutState, actions: FeedScreenActions,
    motion: FeedMapMotion, transition: Transition<Boolean>, padding: PaddingValues, location: GeoPoint?,
    screenSize: Size, roasters: (@Composable () -> Unit)?, shopsFirst: Boolean) {
    val clearance = LocalFloatingNavClearance.current
    val listPadding = PaddingValues(start = CpDimens.spacing4, top = CpDimens.spacing4,
        end = CpDimens.spacing4, bottom = clearance + 88.dp)
    transition.AnimatedContent(Modifier.fillMaxSize().padding(padding).clipToBounds().onGloballyPositioned {
        layout.viewportTop = it.positionInRoot().y - layout.rootOrigin.y
        layout.viewportHeight = it.size.height.toFloat()
    }, transitionSpec = {
        val direction = if (targetState) AnimatedContentTransitionScope.SlideDirection.Down else AnimatedContentTransitionScope.SlideDirection.Up
        slideIntoContainer(direction, tween(360)) togetherWith slideOutOfContainer(direction, tween(360))
    }) { discoveryVisible ->
        if (discoveryVisible) FeedDiscoveryList(state, layout, motion, actions, location, screenSize, roasters)
        else FeedSearchResults(state, layout, actions, location, listPadding, roasters, shopsFirst)
    }
}

@Composable
private fun FeedSearchResults(state: FeedUiState, layout: FeedLayoutState, actions: FeedScreenActions,
    location: GeoPoint?, padding: PaddingValues, roasters: (@Composable () -> Unit)?, shopsFirst: Boolean) {
    val showRoasters = roasters != null && state.query.isNotBlank()
    when {
        state.isLoading && state.shops.isEmpty() && !showRoasters -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
        state.error != null && state.shops.isEmpty() && !showRoasters -> FeedSearchError(state, layout.listState, padding, actions.onRefresh)
        else -> FeedSearchList(state, layout.listState, actions, location, padding, showRoasters, shopsFirst, roasters)
    }
}

@Composable
private fun FeedScreenContentPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FeedScreenContent(FeedUiState(isLoading = false, isSearchActive = true, query = "Coffee"),
        rememberFeedLayout(), FeedScreenActions(), null,
        { _, _, _, modifier -> Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) {} }, null, null)
}

@Preview @Composable private fun FeedScreenContentLightPreview() = FeedScreenContentPreview(false)
@Preview @Composable private fun FeedScreenContentDarkPreview() = FeedScreenContentPreview(true)
