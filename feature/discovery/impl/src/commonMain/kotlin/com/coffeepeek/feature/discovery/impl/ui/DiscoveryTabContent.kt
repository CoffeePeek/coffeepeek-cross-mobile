package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.SearchError
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_continue_checkin

@Composable
internal fun DiscoveryTabContent(
    state: SearchState, host: DiscoveryHost, roasters: DiscoveryRoasterSlots,
    analytics: DiscoveryAnalytics, callbacks: TabCallbacks, bottomClearance: Dp,
    focus: MapFocus?, onAction: (SearchAction) -> Unit, modifier: Modifier = Modifier,
    onSearchPresentation: (listActive: Boolean, mapActive: Boolean) -> Unit,
    map: @Composable (DiscoveryMapPresentation, () -> Unit, Modifier) -> Unit,
) {
    val ui = rememberDiscoveryTabState()
    val reduceMotion = host.reduceMotion()
    val motion = discoveryTabMotion(ui, state.showsDiscovery || ui.mapExpanded, reduceMotion)
    val toggleMap = discoveryMapToggle(ui, analytics)
    DiscoverySearchPresentation(ui, motion, host.isVisible(), onSearchPresentation)
    val context = DiscoveryRoasterContext(state.query, state.filters.roasterIds, state.filters.favoritesOnly)
    DiscoveryTabEffects(ui, state.showsDiscovery, analytics, callbacks, focus, onAction)
    DiscoveryReselectionEffect(ui, state.showsDiscovery, callbacks.reselections, onAction)
    if (host.isVisible() && focus == null && isIdleDiscovery(state, ui, motion)) host.onIdle()
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { ui.rootOrigin = it.positionInRoot() }) {
        val canvasSize = DpSize(maxWidth, maxHeight)
        DiscoverySearchLayer(state, ui, motion, host, roasters, context, bottomClearance,
            callbacks.onAddShop, onAction)
        DiscoveryRoasterOverlay(ui, roasters, context, reduceMotion, onAction)
        if (state.showFilters) FilterSheet(state, host, onAction)
        if (ui.mapAnchor.width > 0f || ui.mapExpanded) DiscoveryMapLayer(ui, motion, host, canvasSize, toggleMap, map)
        if (callbacks.onContinueCheckIn != null && host.isVisible() && !state.showFilters &&
            !ui.showRoasters && !ui.mapExpanded && motion.expansion.value == 0f) {
            DiscoveryContinueCheckIn(callbacks.onContinueCheckIn,
                Modifier.align(Alignment.BottomEnd).padding(horizontal = 16.dp).padding(bottom = bottomClearance + 12.dp))
        }
        if (host.isVisible() && !state.showFilters && !ui.showRoasters &&
            (state.nearbyRequested || state.locationUnavailable || state.locationSettingsNeeded)) {
            DiscoveryLocationNotice(state.nearbyRequested, { onAction(SearchAction.DismissLocationFailure) },
                onOpenSettings = if (state.locationSettingsNeeded) host.openLocationSettings else null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (ui.mapExpanded) 24.dp else bottomClearance + 80.dp))
        }
        if (host.isVisible() && !state.showFilters && !ui.showRoasters && !ui.mapExpanded &&
            !state.nearbyRequested && !state.locationUnavailable && !state.locationSettingsNeeded) {
            FavoriteFailureFeedback(state.favoriteFailureId, { onAction(SearchAction.DismissFavoriteFailure(it)) },
                Modifier.align(Alignment.BottomCenter).padding(bottom = bottomClearance + 80.dp))
        }
    }
}

private fun isIdleDiscovery(state: SearchState, ui: DiscoveryTabState, motion: DiscoveryTabMotion): Boolean =
    state.showsDiscovery && !state.isLoading && !state.isRefreshing && state.error == null &&
        !state.showFilters && !state.nearbyRequested && !state.locationUnavailable &&
        !state.locationSettingsNeeded && !ui.showRoasters && !ui.mapExpanded && motion.expansion.value == 0f &&
        !ui.list.isScrollInProgress && !ui.shops.isScrollInProgress

@Composable
private fun DiscoveryContinueCheckIn(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekFilledButton(onClick = onContinue, modifier = modifier) {
        Text(coffeePeekStringResource(Res.string.discovery_continue_checkin))
    }
}

@Composable
private fun DiscoverySearchPresentation(
    ui: DiscoveryTabState, motion: DiscoveryTabMotion, visible: Boolean, onChange: (Boolean, Boolean) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val callback by rememberUpdatedState(onChange)
    val listActive = visible && !ui.showRoasters && !ui.mapExpanded && motion.expansion.value == 0f
    val mapActive = visible && !ui.showRoasters && ui.mapExpanded && motion.expansion.value == 1f
    DisposableEffect(lifecycle, listActive, mapActive) {
        fun report() {
            val resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            callback(listActive && resumed, mapActive && resumed)
        }
        val observer = LifecycleEventObserver { _, _ -> report() }
        lifecycle.addObserver(observer)
        report()
        onDispose {
            lifecycle.removeObserver(observer)
            callback(false, false)
        }
    }
}

@Composable
private fun DiscoveryTabContentPreview(refreshError: Boolean = false, darkTheme: Boolean = false) = CoffeePeekTheme(darkTheme = darkTheme) {
    DiscoveryTabContent(
        state = SearchState(isLoading = false, shops = listOf(ShopSummary("sandbox-cafe", "Sandbox Cafe")),
            error = if (refreshError) SearchError(null) else null),
        host = DiscoveryHost(),
        roasters = DiscoveryRoasterSlots(preview = {}, search = { _, _, _ -> }),
        analytics = NoDiscoveryAnalytics, callbacks = TabCallbacks({}, {}, {}, {}, {},
            onContinueCheckIn = if (refreshError) null else ({})),
        bottomClearance = 80.dp, focus = null, onAction = {}, onSearchPresentation = { _, _ -> },
    ) { _, _, modifier -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) }
}

@Preview @Composable private fun DiscoveryHomePreview() = DiscoveryTabContentPreview()
@Preview @Composable private fun DiscoveryHomeDarkPreview() = DiscoveryTabContentPreview(darkTheme = true)
@Preview @Composable private fun DiscoveryRefreshErrorPreview() = DiscoveryTabContentPreview(refreshError = true)
