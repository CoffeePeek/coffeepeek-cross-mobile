package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.core.designsystem.component.CoffeePeekLoadErrorContent
import com.coffeepeek.core.designsystem.icons.CoffeePeekIllustrations
import org.jetbrains.compose.resources.painterResource

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_loading
import com.coffeepeek.feature.discovery.impl.resources.discovery_suggestion_title
import com.coffeepeek.feature.discovery.impl.resources.discovery_suggestion_description
import com.coffeepeek.feature.discovery.impl.resources.discovery_suggestion_action
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

@Composable
internal fun DiscoveryShopPreview(
    state: SearchState, host: DiscoveryHost, listState: LazyListState,
    onAddShop: () -> Unit, onAction: (SearchAction) -> Unit,
) {
    val shops = state.visibleShops
    when {
        state.isLoading && shops.isEmpty() -> Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) {
            CoffeePeekLoader(stringResource(Res.string.discovery_loading))
        }
        state.error != null && shops.isEmpty() -> CoffeePeekLoadErrorContent({ onAction(SearchAction.Refresh) }, Modifier)
        shops.isEmpty() -> CoffeePeekCatalogSuggestionCard(
            title = stringResource(Res.string.discovery_suggestion_title),
            description = stringResource(Res.string.discovery_suggestion_description),
            actionLabel = stringResource(Res.string.discovery_suggestion_action),
            illustration = painterResource(CoffeePeekIllustrations.CatalogSearch), onAdd = onAddShop,
            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4),
        )
        else -> LazyRow(
            state = listState, contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4),
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
        ) {
            items(shops.take(PREVIEW_SHOP_COUNT), key = { it.id }) { shop ->
                ShopRow(shop, host, shop.id in state.favoriteUpdates, state.userLocation,
                    onOpen = { onAction(SearchAction.OpenShop(shop.id)) },
                    onFavorite = { onAction(SearchAction.ToggleFavorite(shop.id)) },
                    modifier = Modifier.width(280.dp), showCatalogDetails = false)
            }
        }
    }
}

@Composable
private fun DiscoveryShopPreviewPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    DiscoveryShopPreview(SearchState(isLoading = false, shops = listOf(ShopSummary("sandbox-cafe", "Sandbox Cafe"))),
        DiscoveryHost(), rememberLazyListState(), {}, {})
}

@Preview @Composable private fun DiscoveryShopPreviewPreviewLight() = DiscoveryShopPreviewPreview(false)
@Preview @Composable private fun DiscoveryShopPreviewPreviewDark() = DiscoveryShopPreviewPreview(true)
