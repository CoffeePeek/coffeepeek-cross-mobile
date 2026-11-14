package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.core.media.PrefetchListPhotos

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekPaginationStatus
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.resources.*
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.matchesPickerQuery
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.rules.searchText
import com.coffeepeek.feature.discovery.domain.rules.visibleShops
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInShopPickerList(
    state: SearchState, onAction: (SearchAction) -> Unit, list: LazyListState, modifier: Modifier, reduceMotion: Boolean,
) {
    val applied = state.pickerResultCriteria
    val shops = remember(state.shops, applied) {
        visibleShops(state.shops, applied?.filters ?: state.filters).distinctBy(ShopSummary::id)
    }
    val browse = searchText(state.query) == null && !state.filters.nearbyOnly &&
        searchText(applied?.text.orEmpty()) == null && applied?.filters?.nearbyOnly != true
    val recent = remember(browse, state.pickerRecentShops) {
        if (browse) state.pickerRecentShops.distinctBy(ShopSummary::id) else emptyList()
    }
    val recentIds = remember(recent) { recent.mapTo(mutableSetOf(), ShopSummary::id) }
    val remaining = remember(shops, recentIds) { shops.filterNot { it.id in recentIds } }
    val isPending = applied?.matchesPickerQuery(state.criteria) != true && state.error == null
    KeepPickerTopAtRecents(list, recent.isNotEmpty())
    PickerPagination(state, list, isPending, onAction)
    PrefetchListPhotos(list, (recent + remaining).map { "shop:${it.id}" to listOfNotNull(it.photoUrl) })
    LazyColumn(modifier, state = list,
        contentPadding = coffeePeekScrollContentPadding(PaddingValues(CoffeePeekDimens.spacing4)),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        if (recent.isNotEmpty()) item(key = "recent-heading") {
            Text(stringResource(Res.string.discovery_picker_recent), style = MaterialTheme.typography.titleSmall)
        }
        pickerShopRows(recent, state.userLocation, reduceMotion, onAction)
        pickerShopRows(remaining, state.userLocation, reduceMotion, onAction)
        pickerListStatus(state, shops.isEmpty() && recent.isEmpty(), isPending, onAction)
    }
}

@Composable
private fun KeepPickerTopAtRecents(list: LazyListState, hasRecents: Boolean) {
    var previouslyHadRecents by remember { mutableStateOf(hasRecents) }
    if (hasRecents == previouslyHadRecents) return
    SideEffect {
        previouslyHadRecents = hasRecents
        if (hasRecents && !list.isScrollInProgress && list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0) {
            list.requestScrollToItem(0)
        }
    }
}

@Composable
private fun PickerPagination(state: SearchState, list: LazyListState, isPending: Boolean, onAction: (SearchAction) -> Unit) {
    val nearEnd by remember(list) { derivedStateOf {
        val info = list.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        lastVisible >= 0 && lastVisible >= info.totalItemsCount - 3
    } }
    LaunchedEffect(nearEnd, state.canLoadMore, state.currentPage, isPending) {
        if (nearEnd && state.canLoadMore && !isPending) onAction(SearchAction.LoadMore)
    }
}

private fun LazyListScope.pickerShopRows(
    shops: List<ShopSummary>, location: Coordinates?, reduceMotion: Boolean, onAction: (SearchAction) -> Unit,
) {
    items(shops, key = { "shop:${it.id}" }, contentType = { "shop" }) { shop ->
        val modifier = if (reduceMotion) Modifier else Modifier.animateItem(
            fadeInSpec = tween(140), placementSpec = tween(180), fadeOutSpec = null,
        )
        CheckInShopPickerRow(shop, location, modifier) { onAction(SearchAction.OpenShop(shop.id)) }
    }
}

@Composable
private fun PickerListErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInShopPickerList(SearchState(isLoading = false,
        error = com.coffeepeek.feature.discovery.impl.search.SearchError(null)),
        {}, rememberLazyListState(), Modifier.fillMaxSize(), reduceMotion = true)
}

@Preview @Composable private fun PickerListErrorLightPreview() = PickerListErrorPreview(false)
@Preview @Composable private fun PickerListErrorDarkPreview() = PickerListErrorPreview(true)

private fun LazyListScope.pickerListStatus(state: SearchState, empty: Boolean, isPending: Boolean, onAction: (SearchAction) -> Unit) {
    when {
        state.error != null -> item(key = "search-error") {
            Text(state.error.serverMessage ?: stringResource(Res.string.discovery_picker_failed), color = MaterialTheme.colorScheme.error)
            CoffeePeekTextButton(onClick = { onAction(SearchAction.Refresh) }) { Text(stringResource(Res.string.discovery_picker_retry)) }
        }
        empty && (state.isLoading || state.isRefreshing || isPending) -> item(key = "search-loading") {
            Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.discovery_loading))
            }
        }
        empty && !state.isLoadingMore && !state.hasMore -> item(key = "search-empty") {
            Text(stringResource(Res.string.discovery_picker_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (state.isLoadingMore || state.loadMoreError != null) item(key = "pagination-status") {
        CoffeePeekPaginationStatus(state.isLoadingMore, state.loadMoreError != null,
            onRetry = { onAction(SearchAction.RetryLoadMore) })
    }
}
