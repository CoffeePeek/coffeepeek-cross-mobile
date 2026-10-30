package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.component.CoffeePeekFilterChip
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.component.CoffeePeekCircularBackButton
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchEvent
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel

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
    hasRoasterResults: Boolean? = null,
) {
    val reduceMotion = host.reduceMotion()
    Column(modifier.fillMaxSize()) {
        Column(headerModifier) {
            Column(Modifier.padding(top = CoffeePeekDimens.spacing3, bottom = CoffeePeekDimens.spacing2)) {
                SearchHeader(state, host, onAction)
                Spacer(Modifier.height(CoffeePeekDimens.spacing2))
                DiscoveryQuickFilters(state.filters, state.canUseNearby, onAction)
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
            if (showsDiscovery) discovery() else ResultsList(state, host, onAddShop, bottomClearance, textResults, prefetch, hasRoasterResults, onAction)
        }
    }
}

@Composable
private fun SearchHeader(state: SearchState, host: DiscoveryHost, onAction: (SearchAction) -> Unit) {
    val focus = LocalFocusManager.current
    // The back button shows whenever the results list replaces the home tab, also when only a filter is on.
    val searching = !state.showsDiscovery
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // As in prod: while searching a round back button leaves the search, and the filters stay available beside the field.
        // The button and its gap grow and fade together, so the field slides instead of jumping.
        AnimatedVisibility(
            visible = searching,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoffeePeekCircularBackButton(
                    onClick = { focus.clearFocus(); onAction(SearchAction.CancelSearch) },
                    contentDescription = stringResource(R.string.discovery_search_cancel),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            }
        }
        CoffeePeekSearchField(
            value = state.query,
            onValueChange = { onAction(SearchAction.Query(it)) },
            placeholder = stringResource(R.string.discovery_search_placeholder),
            clearDescription = stringResource(R.string.discovery_search_clear),
            modifier = host.exposure(Modifier.weight(1f).onFocusChanged { if (it.isFocused) onAction(SearchAction.ActivateSearch) }, DiscoveryExposure.Search),
            onSearch = { focus.clearFocus() },
        )
        Spacer(Modifier.width(CoffeePeekDimens.spacing2))
        DiscoveryFilterButton(state.activeFilterCount, { onAction(SearchAction.ToggleFilters) }, host.exposure(Modifier, DiscoveryExposure.Filters))
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
    hasRoasterResults: Boolean?,
    onAction: (SearchAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val shops = state.visibleShops
    val showsTextResults = textResults != null && state.query.isNotBlank()
    val shopsFirst = showsTextResults && state.showsShopsFirst(hasRoasterResults)
    // Section changes must bring the first result into view instead of retaining the old item anchor.
    LaunchedEffect(showsTextResults, shopsFirst) { listState.scrollToItem(0) }
    if (prefetch) host.photoPrefetch(listState, shops.map { it.id to listOfNotNull(it.photoUrl) })
    val nearEnd by remember { derivedStateOf { listState.layoutInfo.let { info -> (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 5 } } }
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
            contentPadding = PaddingValues(start = CoffeePeekDimens.spacing4, top = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4, bottom = bottomClearance + 88.dp),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
        ) {
            if (showsTextResults && !shopsFirst) roasterResults(textResults)
            if (showsTextResults) item(key = "shops-heading") {
                Text(stringResource(R.string.discovery_section_shops), style = MaterialTheme.typography.titleLarge)
            }
            shopResults(state, host, onAddShop, onAction)
            if (shopsFirst) roasterResults(textResults)
        }
    }
}

private fun LazyListScope.roasterResults(content: (@Composable () -> Unit)?) {
    if (content == null) return
    item(key = "text-results-heading") {
        Text(stringResource(R.string.discovery_section_roasters), style = MaterialTheme.typography.titleLarge)
    }
    item(key = "text-results") { content() }
}

private fun LazyListScope.shopResults(
    state: SearchState, host: DiscoveryHost, onAddShop: () -> Unit, onAction: (SearchAction) -> Unit,
) {
    val shops = state.visibleShops
    when {
        state.isLoading && shops.isEmpty() -> item(key = "loading") {
            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(R.string.discovery_loading))
            }
        }
        state.error != null && shops.isEmpty() -> item(key = "error") {
            host.loadError({ onAction(SearchAction.Refresh) }, Modifier.fillParentMaxSize())
        }
        shops.isEmpty() -> item(key = "suggestion") { SuggestionCard(host, onAddShop) }
        else -> shopRows(state, host, onAddShop, onAction)
    }
}

private fun LazyListScope.shopRows(
    state: SearchState, host: DiscoveryHost, onAddShop: () -> Unit, onAction: (SearchAction) -> Unit,
) {
    items(state.visibleShops, key = { it.id }, contentType = { "shop" }) { shop ->
        ShopRow(shop, host, shop.id in state.favoriteUpdates, state.userLocation,
            onOpen = { onAction(SearchAction.OpenShop(shop.id)) },
            onFavorite = { onAction(SearchAction.ToggleFavorite(shop.id)) })
    }
    if (!state.isLoading && state.error == null) item(key = "suggestion") { SuggestionCard(host, onAddShop) }
}

@Composable
private fun SuggestionCard(host: DiscoveryHost, onAddShop: () -> Unit) {
    CoffeePeekCatalogSuggestionCard(
        title = stringResource(R.string.discovery_suggestion_title),
        description = stringResource(R.string.discovery_suggestion_description),
        actionLabel = stringResource(R.string.discovery_suggestion_action),
        illustration = host.suggestionIllustration(),
        onAdd = onAddShop,
    )
}


@Composable
private fun SearchContentPreview(hasRoasterResults: Boolean) = CoffeePeekTheme {
    androidx.compose.material3.Surface {
        SearchContent(
            state = SearchState(query = "Coffee", searchActive = true, isLoading = false,
                shops = listOf(ShopSummary("fixture-shop", "CoffeePeek", rating = 4.7, checkInCount = 12))),
            host = DiscoveryHost(suggestionIllustration = { ColorPainter(Color.Transparent) }),
            onAddShop = {}, bottomClearance = 0.dp, discovery = {}, textResults = {},
            hasRoasterResults = hasRoasterResults, onAction = {},
        )
    }
}

@PreviewLightDark @Composable private fun SearchResultsPreview() = SearchContentPreview(true)
@PreviewLightDark @Composable private fun SearchShopsFirstPreview() = SearchContentPreview(false)
