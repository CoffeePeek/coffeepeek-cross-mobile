package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.component.CoffeePeekFilterChip
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchEvent
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel

internal class SearchCallbacks(val onOpenShop: (String) -> Unit, val onSignIn: () -> Unit, val onAddShop: () -> Unit)

@Composable
internal fun SearchScreen(
    viewModel: SearchViewModel,
    host: DiscoveryHost,
    callbacks: SearchCallbacks,
    bottomClearance: Dp,
    discovery: @Composable () -> Unit,
    textResults: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.OpenShop -> callbacks.onOpenShop(event.shopId)
                SearchEvent.SignInRequired -> callbacks.onSignIn()
            }
        }
    }
    SearchContent(state, host, callbacks.onAddShop, bottomClearance, discovery, textResults, viewModel::onAction, modifier)
    if (state.showFilters) FilterSheet(state, onAction = viewModel::onAction)
}

@Composable
internal fun SearchContent(
    state: SearchState,
    host: DiscoveryHost,
    onAddShop: () -> Unit,
    bottomClearance: Dp,
    discovery: @Composable () -> Unit,
    textResults: (@Composable () -> Unit)?,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
    /** The tab fades and slides the header away while the map expands. */
    headerModifier: Modifier = Modifier,
    /** The tab measures the area below the header, where the map is clipped. */
    bodyModifier: Modifier = Modifier,
    /** No photo prefetch while the map covers the lists. */
    prefetch: Boolean = true,
) {
    val reduceMotion = host.reduceMotion()
    Column(modifier.fillMaxSize()) {
        Column(headerModifier) {
            Column(Modifier.padding(top = CoffeePeekDimens.spacing3, bottom = CoffeePeekDimens.spacing2)) {
                SearchHeader(state, host, onAction)
                QuickFilters(state, onAction)
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        }
        AnimatedContent(
            targetState = state.showsDiscovery,
            modifier = Modifier.weight(1f).fillMaxWidth().then(bodyModifier).clipToBounds(),
            transitionSpec = {
                val direction = if (targetState) AnimatedContentTransitionScope.SlideDirection.Down else AnimatedContentTransitionScope.SlideDirection.Up
                val spec = tween<IntOffset>(if (reduceMotion) 0 else 360)
                slideIntoContainer(direction, spec) togetherWith slideOutOfContainer(direction, spec)
            },
            label = "discovery-shop-list",
        ) { showsDiscovery ->
            if (showsDiscovery) discovery() else ResultsList(state, host, onAddShop, bottomClearance, textResults, prefetch, onAction)
        }
    }
}

@Composable
private fun SearchHeader(state: SearchState, host: DiscoveryHost, onAction: (SearchAction) -> Unit) {
    val focus = LocalFocusManager.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        CoffeePeekSearchField(
            value = state.query,
            onValueChange = { onAction(SearchAction.Query(it)) },
            placeholder = stringResource(R.string.discovery_search_placeholder),
            clearDescription = stringResource(R.string.discovery_search_clear),
            modifier = host.exposure(Modifier.weight(1f).onFocusChanged { if (it.isFocused) onAction(SearchAction.ActivateSearch) }, DiscoveryExposure.Search),
            onSearch = { focus.clearFocus() },
        )
        if (state.searchActive || state.query.isNotBlank()) {
            TextButton(onClick = { focus.clearFocus(); onAction(SearchAction.CancelSearch) }) { Text(stringResource(R.string.discovery_search_cancel)) }
        } else {
            val description = if (state.activeFilterCount > 0) stringResource(R.string.discovery_filters_active, state.activeFilterCount) else stringResource(R.string.discovery_filters)
            Box(host.exposure(Modifier, DiscoveryExposure.Filters)) {
                IconButton(onClick = { onAction(SearchAction.ToggleFilters) }, modifier = Modifier.semantics { contentDescription = description }) {
                    Icon(CoffeePeekIcons.Filter, contentDescription = null)
                }
                if (state.activeFilterCount > 0) Badge(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)) { Text(state.activeFilterCount.toString()) }
            }
        }
    }
}

@Composable
private fun QuickFilters(state: SearchState, onAction: (SearchAction) -> Unit) {
    val filters = state.filters
    LazyRow(
        contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        modifier = Modifier.fillMaxWidth().padding(bottom = CoffeePeekDimens.spacing2),
    ) {
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_open), filters.openOnly, { onAction(SearchAction.ToggleOpen) }) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_new), filters.newOnly, { onAction(SearchAction.ToggleNew) }) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_visited), filters.visitedOnly, { onAction(SearchAction.ToggleVisited) }) }
        item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_favorites), filters.favoritesOnly, { onAction(SearchAction.ToggleFavorites) }) }
        if (state.canUseNearby) item { CoffeePeekFilterChip(stringResource(R.string.discovery_quick_nearby), filters.nearbyOnly, { onAction(SearchAction.ToggleNearby) }) }
    }
}

@Composable
private fun ResultsList(
    state: SearchState,
    host: DiscoveryHost,
    onAddShop: () -> Unit,
    bottomClearance: Dp,
    textResults: (@Composable () -> Unit)?,
    prefetch: Boolean,
    onAction: (SearchAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val shops = state.visibleShops
    if (prefetch) host.photoPrefetch(listState, shops.map { it.id to listOfNotNull(it.photoUrl) })
    val nearEnd by remember { derivedStateOf { listState.layoutInfo.let { info -> (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 3 } } }
    LaunchedEffect(nearEnd, state.hasMore, state.isLoadingMore, shops.size) { if (nearEnd && state.hasMore && shops.isNotEmpty()) onAction(SearchAction.LoadMore) }
    CoffeePeekPullToRefresh(
        listState = listState,
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(SearchAction.Refresh) },
        refreshDescription = stringResource(R.string.discovery_refresh),
        loadingDescription = stringResource(R.string.discovery_refreshing),
    ) { scrollModifier ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier.fillMaxSize(),
            contentPadding = PaddingValues(start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4, bottom = bottomClearance + CoffeePeekDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
        ) {
            if (textResults != null && state.query.isNotBlank()) {
                item(key = "text-results-heading") { Text(stringResource(R.string.discovery_section_roasters), style = MaterialTheme.typography.titleLarge) }
                item(key = "text-results") { textResults() }
                item(key = "shops-heading") { Text(stringResource(R.string.discovery_section_shops), style = MaterialTheme.typography.titleMedium) }
            }
            when {
                state.isLoading && shops.isEmpty() -> item(key = "loading") { Centered { CoffeePeekLoader(stringResource(R.string.discovery_loading)) } }
                state.error != null && shops.isEmpty() -> item(key = "error") {
                    Centered {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                            Text(state.error.serverMessage ?: stringResource(R.string.discovery_load_failed), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                            CoffeePeekButton(stringResource(R.string.discovery_retry), { onAction(SearchAction.Refresh) })
                        }
                    }
                }
                shops.isEmpty() -> item(key = "empty") {
                    Centered {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                            Text(stringResource(R.string.discovery_empty), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.discovery_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
                else -> {
                    items(shops, key = { it.id }, contentType = { "shop" }) { shop ->
                        ShopRow(shop, host, shop.id in state.favoriteUpdates, onOpen = { onAction(SearchAction.OpenShop(shop.id)) }, onFavorite = { onAction(SearchAction.ToggleFavorite(shop.id)) })
                    }
                    if (state.isLoadingMore) item(key = "more") { Centered { CoffeePeekLoader(stringResource(R.string.discovery_loading), size = 28.dp) } }
                    else if (!state.hasMore) item(key = "suggestion") {
                        CoffeePeekCatalogSuggestionCard(
                            title = stringResource(R.string.discovery_suggestion_title),
                            description = stringResource(R.string.discovery_suggestion_description),
                            actionLabel = stringResource(R.string.discovery_suggestion_action),
                            illustration = host.suggestionIllustration(),
                            onAdd = onAddShop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = CoffeePeekDimens.spacing8), contentAlignment = Alignment.Center) { content() }
}
