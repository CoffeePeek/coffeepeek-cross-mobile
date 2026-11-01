package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import coffeepeek.composeapp.generated.resources.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.ShopLocation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapShopCarousel(onOpenShop: (String) -> Unit, state: MapUiState, onSelect: (MapShop) -> Unit, modifier: Modifier = Modifier) {
    val selected = state.selectedShop
    val shops = if (selected == null || state.nearbyShops.any { it.id == selected.id }) state.nearbyShops else
        listOf(selected) + state.nearbyShops.take(9)
    if (shops.isEmpty()) return
    key(shops.map { it.id }) {
        val selectedIndex = shops.indexOfFirst { it.id == selected?.id }.coerceAtLeast(0)
        val pager = rememberPagerState(initialPage = carouselStartPage(shops.size, selectedIndex),
            pageCount = { if (shops.size > 1) Int.MAX_VALUE else 1 })
        MapCarouselEffects(pager, shops, selected?.id, selectedIndex, onSelect)
        val description = stringResource(Res.string.legacy_map_carousel_position, pager.currentPage % shops.size + 1, shops.size)
        HorizontalPager(pager, modifier.fillMaxWidth().semantics {
            collectionInfo = CollectionInfo(1, shops.size)
            stateDescription = description
        }, contentPadding = PaddingValues(horizontal = 36.dp), pageSpacing = 12.dp,
            userScrollEnabled = shops.size > 1, beyondViewportPageCount = 1) { page ->
            MapCarouselCard(shops[page % shops.size], state, onOpenShop, onSelect)
        }
    }
}

@Composable
private fun MapCarouselEffects(pager: PagerState, shops: List<MapShop>, selectedId: String?, selectedIndex: Int, onSelect: (MapShop) -> Unit) {
    val currentSelection by rememberUpdatedState(selectedId)
    LaunchedEffect(selectedId) {
        if (selectedId == null) return@LaunchedEffect
        val currentIndex = pager.currentPage % shops.size
        if (currentIndex != selectedIndex) {
            val right = (selectedIndex - currentIndex + shops.size) % shops.size
            val delta = if (right <= shops.size / 2) right else right - shops.size
            pager.animateScrollToPage(pager.currentPage + delta)
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().drop(1).collect { page ->
            val shop = shops[page % shops.size]
            if (shop.id != currentSelection) onSelect(shop)
        }
    }
}

@Composable
private fun MapCarouselCard(shop: MapShop, state: MapUiState, onOpenShop: (String) -> Unit, onSelect: (MapShop) -> Unit) {
    val isSelected = shop.id == state.selectedShop?.id
    MapShopCard(shop, state.cardDetails[shop.id] ?: state.selectedShopDetails.takeIf { isSelected },
        isLoadingDetails = isSelected && state.isLoadingShopDetails,
        distance = formatDistance(distanceToShopMeters(state.nearbyOrigin, ShopLocation("", shop.latitude, shop.longitude))),
        onOpen = { if (isSelected) onOpenShop(shop.id) else onSelect(shop) })
}

@Composable
private fun MapShopCarouselPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    MapShopCarousel({}, MapUiState(nearbyShops = listOf(MapShop("preview", "CoffeePeek", 53.9, 27.56))), {})
}

@Preview @Composable private fun MapShopCarouselLightPreview() = MapShopCarouselPreview(false)
@Preview @Composable private fun MapShopCarouselDarkPreview() = MapShopCarouselPreview(true)
