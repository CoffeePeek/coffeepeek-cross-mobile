package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.R
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
            CoffeePeekLoader(stringResource(R.string.discovery_loading))
        }
        state.error != null && shops.isEmpty() -> host.loadError({ onAction(SearchAction.Refresh) }, Modifier)
        shops.isEmpty() -> CoffeePeekCatalogSuggestionCard(
            title = stringResource(R.string.discovery_suggestion_title),
            description = stringResource(R.string.discovery_suggestion_description),
            actionLabel = stringResource(R.string.discovery_suggestion_action),
            illustration = host.suggestionIllustration(), onAdd = onAddShop,
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

@PreviewLightDark
@Composable
private fun DiscoveryShopPreviewPreview() = CoffeePeekTheme {
    DiscoveryShopPreview(SearchState(isLoading = false, shops = listOf(ShopSummary("sandbox-cafe", "Sandbox Cafe"))),
        DiscoveryHost(), rememberLazyListState(), {}, {})
}
