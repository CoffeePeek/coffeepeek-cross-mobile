package com.coffeepeek.admin.ui.screen.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.core.designsystem.component.CoffeePeekMapPreviewOverlay
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.MapShop
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapScreenContent(
    state: MapUiState,
    actions: MapScreenActions,
    isPreview: Boolean,
    filterControl: (@Composable () -> Unit)?,
    filterBar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    mapContent: @Composable (Modifier) -> Unit,
) {
    var carouselHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val clearance = LocalFloatingNavClearance.current
    val hasCarousel = state.selectedShop != null || state.nearbyShops.isNotEmpty()
    Box(modifier.fillMaxSize()) {
        mapContent(Modifier.align(Alignment.Center))
        if (isPreview) {
            CoffeePeekMapPreviewOverlay(actions.onExpand, Modifier.matchParentSize())
        } else {
            MapHeader(state, actions, filterControl, filterBar, Modifier.align(Alignment.TopCenter))
            MapPositionControls(state, actions, hasCarousel, carouselHeight, clearance)
            if (state.selectedZone == null && hasCarousel) {
                MapShopCarousel(actions.onOpenShop, state, actions.onCarouselSelect,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = clearance + CpDimens.spacing4)
                        .onSizeChanged { carouselHeight = with(density) { it.height.toDp() } })
            }
            state.selectedZone?.let { zone ->
                MapZoneCard(zone, actions.onShowZoneShops, actions.onDismissZone,
                    Modifier.align(Alignment.BottomCenter).padding(
                        start = CpDimens.spacing4, end = CpDimens.spacing4, bottom = clearance + CpDimens.spacing4))
            }
        }
    }
}

@Composable
private fun MapScreenContentPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val shop = MapShop("preview", "CoffeePeek", 53.9, 27.56)
    val details = CoffeeShopDetails(CoffeeShop("preview", "CoffeePeek", 4.5, cityName = null, priceRange = null, photoUrl = null))
    MapScreenContent(MapUiState(nearbyShops = listOf(shop), selectedShop = shop,
        cardDetails = mapOf(shop.id to details)),
        MapScreenActions(onCollapse = {}), false, { DiscoveryMapFiltersButton(2, {}) }, null,
        Modifier.size(390.dp, 780.dp)) { Surface(it.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {} }
}

@Preview @Composable private fun MapScreenContentLightPreview() = MapScreenContentPreview(false)
@Preview @Composable private fun MapScreenContentDarkPreview() = MapScreenContentPreview(true)
