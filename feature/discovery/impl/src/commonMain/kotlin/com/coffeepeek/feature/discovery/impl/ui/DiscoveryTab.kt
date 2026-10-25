package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchEvent
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel

internal class TabCallbacks(
    val onOpenShop: (String) -> Unit,
    val onSignIn: () -> Unit,
    val onAddShop: () -> Unit,
    val onMapExpandedChange: (Boolean) -> Unit,
    val onFocusConsumed: () -> Unit,
)

/** Lifecycle adapter; layout and transient overlays belong to the stateless content. */
@Composable
internal fun DiscoveryTab(
    searchViewModel: SearchViewModel,
    mapViewModel: MapViewModel,
    host: DiscoveryHost,
    roasters: DiscoveryRoasterSlots,
    analytics: DiscoveryAnalytics,
    callbacks: TabCallbacks,
    bottomClearance: Dp,
    focus: MapFocus?,
    modifier: Modifier = Modifier,
) {
    val state by searchViewModel.state.collectAsStateWithLifecycle()
    SearchNavigationEvents(searchViewModel, callbacks)
    DiscoveryTabContent(state, host, roasters, analytics, callbacks, bottomClearance, focus,
        searchViewModel::onAction, modifier) { presentation, onToggleExpand, mapModifier ->
        MapScreen(mapViewModel, host, callbacks.onOpenShop, bottomClearance,
            focus.takeIf { presentation.expanded }, callbacks.onFocusConsumed,
            modifier = mapModifier, isPreview = !presentation.controlsExpanded,
            onToggleExpand = onToggleExpand, canvasSize = presentation.canvasSize)
    }
}

@Composable
private fun SearchNavigationEvents(viewModel: SearchViewModel, callbacks: TabCallbacks) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.OpenShop -> callbacks.onOpenShop(event.shopId)
                SearchEvent.SignInRequired -> callbacks.onSignIn()
            }
        }
    }
}
