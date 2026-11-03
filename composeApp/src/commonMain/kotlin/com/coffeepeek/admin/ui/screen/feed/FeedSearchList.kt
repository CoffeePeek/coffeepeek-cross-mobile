package com.coffeepeek.admin.ui.screen.feed

import coffeepeek.composeapp.generated.resources.*

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
import com.coffeepeek.core.designsystem.component.CoffeePeekRefreshError
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CoffeeShop
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.feed_refresh_error
import coffeepeek.composeapp.generated.resources.retry

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
            if (state.showsRefreshError) item(key = "refresh-error") {
                CoffeePeekRefreshError(onRetry = actions.onRefresh,
                    message = stringResource(Res.string.feed_refresh_error), retryLabel = stringResource(Res.string.retry))
            }
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
    item(key = "shop-search-heading") { Text(stringResource(Res.string.profile_stat_shops), style = MaterialTheme.typography.titleLarge) }
    when {
        state.isLoading && state.shops.isEmpty() -> item(key = "shop-search-loading") {
            Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
        }
        state.error != null && state.shops.isEmpty() -> item(key = "shop-search-error") { LoadErrorContent(onRetry = refresh) }
    }
}

private fun LazyListScope.roasterSearchResults(content: (@Composable () -> Unit)?) {
    if (content == null) return
    item(key = "roaster-search-heading") { DiscoverySectionTitle(stringResource(Res.string.favorites_roasters), null) }
    item(key = "roaster-search-results") { content() }
}

@Composable
private fun FeedSearchListPreview(dark: Boolean, paginationError: Boolean = false, refreshError: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    Surface(color = MaterialTheme.colorScheme.background) {
        val shops = listOf(CoffeeShop("preview", "CoffeePeek", 4.5, cityName = null, priceRange = null, photoUrl = null))
        FeedSearchList(FeedUiState(isLoading = false, shops = shops, loadMoreFailed = paginationError,
            error = if (refreshError) "preview" else null), rememberLazyListState(), FeedScreenActions(),
            null, PaddingValues(16.dp), false, false, null)
    }
}

@Preview @Composable private fun FeedSearchListLightPreview() = FeedSearchListPreview(false)
@Preview @Composable private fun FeedSearchListDarkPreview() = FeedSearchListPreview(true)
@Preview @Composable private fun FeedPaginationErrorLightPreview() = FeedSearchListPreview(false, paginationError = true)
@Preview @Composable private fun FeedPaginationErrorDarkPreview() = FeedSearchListPreview(true, paginationError = true)
@Preview @Composable private fun FeedRefreshErrorLightPreview() = FeedSearchListPreview(false, refreshError = true)
@Preview @Composable private fun FeedRefreshErrorDarkPreview() = FeedSearchListPreview(true, refreshError = true)
