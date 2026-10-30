package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.feature.catalog.api.ShopCard
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.LoadErrorContent
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.component.CoffeePeekPaginationStatus
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FeedSearchList(state: FeedUiState, listState: LazyListState, actions: FeedScreenActions,
    location: GeoPoint?, padding: PaddingValues, showRoasters: Boolean, shopsFirst: Boolean, roasters: (@Composable () -> Unit)?) {
    CoffeePeekPullToRefresh(listState = listState, isRefreshing = state.isRefreshing,
        onRefresh = actions.onRefresh, modifier = Modifier.fillMaxSize()) { scrollModifier ->
        LazyColumn(state = listState, modifier = scrollModifier.fillMaxSize(),
            contentPadding = coffeePeekScrollContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            if (showRoasters && !shopsFirst) roasterSearchResults(roasters)
            if (showRoasters) feedShopSearchHeading(state, actions.onRefresh)
            items(state.visibleShops, key = { it.id }) { shop ->
                ShopCard(shop = shop, distance = formatDistance(shop.distanceMeters ?: distanceToShopMeters(location, shop.location)),
                    onClick = { actions.onOpenShop(shop) }, onToggleFavorite = { actions.onToggleFavorite(shop) })
            }
            if (state.isLoadingMore || state.loadMoreFailed) item(key = "pagination-status") {
                CoffeePeekPaginationStatus(state.isLoadingMore, state.loadMoreFailed, actions.onRetryLoadMore)
            }
            if (shopsFirst) roasterSearchResults(roasters)
            if (!state.isLoading && state.error == null && !state.loadMoreFailed) item(key = "add-missing-shop") {
                CatalogSuggestionCard(CatalogSuggestionKind.Shop, onAdd = actions.onAddShop)
            }
        }
    }
}

@Composable
internal fun FeedSearchError(state: FeedUiState, listState: LazyListState, padding: PaddingValues, refresh: () -> Unit) {
    CoffeePeekPullToRefresh(listState = listState, isRefreshing = state.isRefreshing,
        onRefresh = refresh, modifier = Modifier.fillMaxSize()) { scrollModifier ->
        LazyColumn(state = listState, modifier = scrollModifier.fillMaxSize(),
            contentPadding = coffeePeekScrollContentPadding(padding), verticalArrangement = Arrangement.Center) {
            item { LoadErrorContent(onRetry = refresh, modifier = Modifier.fillParentMaxSize()) }
        }
    }
}

private fun LazyListScope.feedShopSearchHeading(state: FeedUiState, refresh: () -> Unit) {
    item(key = "shop-search-heading") { Text("Кофейни", style = MaterialTheme.typography.titleLarge) }
    when {
        state.isLoading && state.shops.isEmpty() -> item(key = "shop-search-loading") {
            Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
        }
        state.error != null && state.shops.isEmpty() -> item(key = "shop-search-error") { LoadErrorContent(onRetry = refresh) }
    }
}

private fun LazyListScope.roasterSearchResults(content: (@Composable () -> Unit)?) {
    if (content == null) return
    item(key = "roaster-search-heading") { DiscoverySectionTitle("Обжарщики", null) }
    item(key = "roaster-search-results") { content() }
}

@Composable
private fun FeedSearchListPreview(dark: Boolean, paginationError: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    Surface(color = MaterialTheme.colorScheme.background) {
        FeedSearchList(FeedUiState(isLoading = false, loadMoreFailed = paginationError), rememberLazyListState(), FeedScreenActions(),
            null, PaddingValues(16.dp), false, false, null)
    }
}

@Preview @Composable private fun FeedSearchListLightPreview() = FeedSearchListPreview(false)
@Preview @Composable private fun FeedSearchListDarkPreview() = FeedSearchListPreview(true)
@Preview @Composable private fun FeedPaginationErrorLightPreview() = FeedSearchListPreview(false, paginationError = true)
@Preview @Composable private fun FeedPaginationErrorDarkPreview() = FeedSearchListPreview(true, paginationError = true)
