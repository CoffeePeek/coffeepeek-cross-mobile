package com.coffeepeek.admin.ui.screen.feed

import coffeepeek.composeapp.generated.resources.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.feature.catalog.api.ShopCard
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.LoadErrorContent
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.component.CoffeePeekRefreshError
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CoffeeShop
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.feed_refresh_error
import coffeepeek.composeapp.generated.resources.retry

@Composable
internal fun FeedDiscoveryList(state: FeedUiState, layout: FeedLayoutState, motion: FeedMapMotion,
    actions: FeedScreenActions, location: GeoPoint?, screenSize: Size, roasterPreview: (@Composable () -> Unit)?) {
        val localizedProfileStatShops = stringResource(Res.string.profile_stat_shops)
        val localizedFavoritesRoasters = stringResource(Res.string.favorites_roasters)

    val clearance = LocalFloatingNavClearance.current
    CoffeePeekPullToRefresh(listState = layout.discoveryListState, isRefreshing = state.isRefreshing,
        onRefresh = actions.onRefresh, modifier = Modifier.fillMaxSize()
            .then(if (layout.mapExpanded) Modifier.clearAndSetSemantics {} else Modifier).graphicsLayer {
                alpha = 1f - motion.expansion.value
                translationY = screenSize.height * 0.3f * motion.expansion.value
            }) { scrollModifier ->
        LazyColumn(state = layout.discoveryListState, userScrollEnabled = !layout.mapExpanded,
            modifier = scrollModifier.fillMaxSize(),
            contentPadding = coffeePeekScrollContentPadding(PaddingValues(top = CpDimens.spacing4, bottom = clearance + 88.dp)),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
            item(key = "mini-map") { FeedMapAnchor(layout, motion, state.showDiscovery) }
            item(key = "shops-heading") {
                Box(Modifier.analyticsExposure("shop_catalog", "discovery", "discovery")) {
                    DiscoverySectionTitle(localizedProfileStatShops, actions.onActivateSearch, Modifier.padding(horizontal = CpDimens.spacing4))
                }
            }
            if (state.showsRefreshError) item(key = "refresh-error") {
                CoffeePeekRefreshError(onRetry = actions.onRefresh,
                    message = stringResource(Res.string.feed_refresh_error), retryLabel = stringResource(Res.string.retry))
            }
            item(key = "shops-preview") { FeedShopPreview(state, layout, actions, location) }
            if (roasterPreview != null) {
                item(key = "roasters-heading") {
                    DiscoverySectionTitle(localizedFavoritesRoasters, actions.onSelectRoasters, Modifier.padding(horizontal = CpDimens.spacing4))
                }
                item(key = "roasters-preview") { roasterPreview() }
            }
        }
    }
}

@Composable
private fun FeedMapAnchor(layout: FeedLayoutState, motion: FeedMapMotion, showDiscovery: Boolean) {
    Box(Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing4).height(240.dp).onGloballyPositioned {
        if (showDiscovery && motion.progress.value == 1f && !layout.mapExpanded && motion.expansion.value == 0f) {
            layout.mapAnchor = Rect(it.positionInRoot() - layout.rootOrigin, Size(it.size.width.toFloat(), it.size.height.toFloat()))
        }
    })
}

@Composable
private fun FeedShopPreview(state: FeedUiState, layout: FeedLayoutState, actions: FeedScreenActions, location: GeoPoint?) {
    when {
        state.isLoading && state.shops.isEmpty() -> Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) {
            CoffeePeekLoader()
        }
        state.error != null && state.shops.isEmpty() -> LoadErrorContent(onRetry = actions.onRefresh)
        state.visibleShops.isEmpty() -> CatalogSuggestionCard(CatalogSuggestionKind.Shop,
            onAdd = actions.onAddShop, modifier = Modifier.padding(horizontal = CpDimens.spacing4))
        else -> LazyRow(state = layout.discoveryShopsState, contentPadding = PaddingValues(horizontal = CpDimens.spacing4),
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            items(state.visibleShops.take(8), key = { it.id }) { shop ->
                ShopCard(shop = shop, distance = formatDistance(shop.distanceMeters ?: distanceToShopMeters(location, shop.location)),
                    onClick = { actions.onOpenShop(shop) }, onToggleFavorite = { actions.onToggleFavorite(shop) },
                    modifier = Modifier.width(280.dp), showCatalogDetails = false)
            }
        }
    }
}

@Composable
private fun FeedDiscoveryListPreview(dark: Boolean, refreshError: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    Surface(color = MaterialTheme.colorScheme.background) {
        val shops = listOf(CoffeeShop("preview", "CoffeePeek", 4.5, cityName = null, priceRange = null, photoUrl = null))
        FeedDiscoveryList(FeedUiState(isLoading = false, shops = shops, error = if (refreshError) "preview" else null), rememberFeedLayout(),
            FeedMapMotion(rememberUpdatedState(1f), rememberUpdatedState(0f)), FeedScreenActions(), null, Size(390f, 780f), null)
    }
}

@Preview @Composable private fun FeedDiscoveryListLightPreview() = FeedDiscoveryListPreview(false)
@Preview @Composable private fun FeedDiscoveryListDarkPreview() = FeedDiscoveryListPreview(true)
@Preview @Composable private fun FeedDiscoveryRefreshErrorLightPreview() = FeedDiscoveryListPreview(false, refreshError = true)
@Preview @Composable private fun FeedDiscoveryRefreshErrorDarkPreview() = FeedDiscoveryListPreview(true, refreshError = true)
