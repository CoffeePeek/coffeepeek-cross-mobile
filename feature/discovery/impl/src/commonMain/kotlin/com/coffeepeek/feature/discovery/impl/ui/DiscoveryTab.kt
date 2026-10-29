package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
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
    LaunchedEffect(searchViewModel, mapViewModel) {
        searchViewModel.state.map { it.criteria }.distinctUntilChanged().collect { mapViewModel.onAction(MapAction.SyncCriteria(it)) }
    }
    SearchNavigationEvents(searchViewModel, callbacks)
    DiscoveryTabContent(state, host, roasters, analytics, callbacks, bottomClearance, focus,
        searchViewModel::onAction, modifier, onSearchPresentation = { listActive, mapActive ->
            searchViewModel.onAction(SearchAction.PresentationChanged(listActive))
            mapViewModel.onAction(MapAction.PresentationChanged(mapActive))
        }) { presentation, onToggleExpand, mapModifier ->
        MapScreen(mapViewModel, host, callbacks.onOpenShop, bottomClearance,
            focus.takeIf { presentation.expanded }, callbacks.onFocusConsumed,
            modifier = mapModifier, isPreview = !presentation.controlsExpanded,
            onToggleExpand = onToggleExpand, canvasSize = presentation.canvasSize,
            onOpenFilters = { searchViewModel.onAction(SearchAction.ToggleFilters) }, onSearchAction = searchViewModel::onAction)
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
