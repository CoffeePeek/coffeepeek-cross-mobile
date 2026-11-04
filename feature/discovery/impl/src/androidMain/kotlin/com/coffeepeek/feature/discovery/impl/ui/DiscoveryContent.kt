package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.component.CoffeePeekRefreshError
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.tab.Frame

internal const val PREVIEW_SHOP_COUNT = 8

/** The mini-map slot, shop carousel and roaster preview share the discovery scroll state. */
@Composable
internal fun DiscoveryContent(
    state: SearchState, host: DiscoveryHost, roasters: DiscoveryRoasterSlots, context: DiscoveryRoasterContext,
    listState: LazyListState, shopsState: LazyListState, mapExpanded: Boolean, expansion: Float,
    bottomClearance: Dp, onAddShop: () -> Unit, showAllRoasters: () -> Unit,
    onAction: (SearchAction) -> Unit, onMapSlot: (Frame, Boolean) -> Unit,
) {
    val photos = state.visibleShops.take(PREVIEW_SHOP_COUNT).map { it.id to listOfNotNull(it.photoUrl) }
    host.photoPrefetch(shopsState, photos.takeIf { !mapExpanded }.orEmpty())
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val miniMapHeight = (maxHeight * 0.3f).coerceIn(180.dp, 200.dp)
        CoffeePeekPullToRefresh(
            listState = listState, isRefreshing = state.isRefreshing,
            onRefresh = { onAction(SearchAction.Refresh) },
            refreshDescription = stringResource(R.string.discovery_refresh),
            loadingDescription = stringResource(R.string.discovery_refreshing),
            indicatorTopOffset = CoffeePeekDimens.spacing4 + miniMapHeight,
            modifier = Modifier.fillMaxSize()
                .then(if (mapExpanded) Modifier.clearAndSetSemantics {} else Modifier)
                .graphicsLayer {
                    alpha = 1f - expansion
                    translationY = size.height * 0.3f * expansion
                },
        ) { scrollModifier ->
            DiscoveryLandingList(state, host, roasters, context, listState, shopsState, mapExpanded,
                bottomClearance, onAddShop, showAllRoasters, onAction, onMapSlot, miniMapHeight, scrollModifier)
        }
    }
}

@Composable
private fun DiscoveryLandingList(
    state: SearchState, host: DiscoveryHost, roasters: DiscoveryRoasterSlots, context: DiscoveryRoasterContext,
    listState: LazyListState, shopsState: LazyListState, mapExpanded: Boolean, bottomClearance: Dp,
    onAddShop: () -> Unit, showAllRoasters: () -> Unit, onAction: (SearchAction) -> Unit,
    onMapSlot: (Frame, Boolean) -> Unit, miniMapHeight: Dp, modifier: Modifier,
) {
    LazyColumn(
        state = listState, userScrollEnabled = !mapExpanded, modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = CoffeePeekDimens.spacing4, bottom = bottomClearance + 88.dp),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
    ) {
        item(key = MAP_ITEM_KEY) { DiscoveryMapSlot(miniMapHeight, onMapSlot) }
        item(key = "shops-heading") {
            Box(host.exposure(Modifier, DiscoveryExposure.ShopCatalog)) {
                DiscoverySectionTitle(stringResource(R.string.discovery_section_shops),
                    { onAction(SearchAction.ActivateSearch) }, Modifier.padding(horizontal = CoffeePeekDimens.spacing4))
            }
        }
        if (state.showsRefreshError) item(key = "refresh-error") {
            CoffeePeekRefreshError(onRetry = { onAction(SearchAction.Refresh) },
                message = stringResource(R.string.discovery_refresh_error), retryLabel = stringResource(R.string.discovery_retry))
        }
        item(key = "shops-preview") { DiscoveryShopPreview(state, host, shopsState, onAddShop, onAction) }
        item(key = "roasters-heading") {
            DiscoverySectionTitle(stringResource(R.string.discovery_section_roasters), showAllRoasters,
                Modifier.padding(horizontal = CoffeePeekDimens.spacing4))
        }
        item(key = "roasters-preview") { roasters.preview(context) }
    }
}

@Composable
private fun DiscoveryMapSlot(height: Dp, onMapSlot: (Frame, Boolean) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4).height(height).onGloballyPositioned {
        onMapSlot(Frame.of(it.positionInRoot().x, it.positionInRoot().y, it.size.width.toFloat(), it.size.height.toFloat()), true)
    })
}

@PreviewLightDark
@Composable
private fun DiscoveryContentCompactPreview() = CoffeePeekTheme {
    Box(Modifier.width(320.dp).height(640.dp)) {
        DiscoveryContent(
            state = SearchState(isLoading = false, shops = listOf(ShopSummary("sandbox", "Sandbox Cafe"))),
            host = DiscoveryHost(), roasters = DiscoveryRoasterSlots(preview = {}, search = { _, _, _ -> }),
            context = DiscoveryRoasterContext("", emptySet(), false),
            listState = rememberLazyListState(), shopsState = rememberLazyListState(),
            mapExpanded = false, expansion = 0f, bottomClearance = 80.dp,
            onAddShop = {}, showAllRoasters = {}, onAction = {}, onMapSlot = { _, _ -> },
        )
    }
}
