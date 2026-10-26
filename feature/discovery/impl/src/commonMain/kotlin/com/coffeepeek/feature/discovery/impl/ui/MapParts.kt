package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.background
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_loading
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_checkins
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_closed
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_hours
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_loading
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_nearby
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_no_checkins
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_open
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_carousel_position
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_photo_missing
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_search_empty
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_search_failed
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zone_close
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zone_shops_few
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zone_shops_many
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zone_shops_one
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zone_show_shops
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zoom_in
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zoom_out
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.discovery.impl.ui.compose.component.DiscoveryMapShopCard
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.MapZone
import com.coffeepeek.feature.discovery.domain.rules.distanceMeters
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@Composable
internal fun MapSuggestions(state: MapState, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radiusLg), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        when {
            state.isSearchLoading -> Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.discovery_loading), size = 24.dp)
            }
            state.searchFailed -> SuggestionText(Res.string.discovery_map_search_failed)
            state.suggestions.isEmpty() -> SuggestionText(Res.string.discovery_map_search_empty)
            else -> Column {
                state.suggestions.forEachIndexed { index, shop ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onSelect(shop.id) }.padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
                    ) {
                        Icon(CoffeePeekIcons.Location, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(shop.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            shop.address?.takeIf(String::isNotBlank)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index != state.suggestions.lastIndex) HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SuggestionText(text: StringResource) {
    Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(CoffeePeekDimens.spacing4))
}

@Composable
internal fun MapZoomControl(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusLg)
    Column(modifier.width(CoffeePeekDimens.buttonHeight).clip(shape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)).border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
        ZoomButton(CoffeePeekIcons.Add, Res.string.discovery_map_zoom_in, onZoomIn)
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        ZoomButton(CoffeePeekIcons.Minus, Res.string.discovery_map_zoom_out, onZoomOut)
    }
}

@Composable
private fun ZoomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: StringResource, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, stringResource(description), Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun MapZoneCard(zone: MapZone, onShowShops: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(zone.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(zoneShopCount(zone.shopCount), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onDismiss) { Icon(CoffeePeekIcons.Close, stringResource(Res.string.discovery_map_zone_close)) }
            }
            if (zone.description.isNotBlank()) {
                Text(zone.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Button(onShowShops, Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight), shape = RoundedCornerShape(percent = 50)) {
                Text(stringResource(Res.string.discovery_map_zone_show_shops))
            }
        }
    }
}

@Composable
private fun zoneShopCount(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return stringResource(
        when {
            mod100 in 11..14 -> Res.string.discovery_map_zone_shops_many
            mod10 == 1 -> Res.string.discovery_map_zone_shops_one
            mod10 in 2..4 -> Res.string.discovery_map_zone_shops_few
            else -> Res.string.discovery_map_zone_shops_many
        },
        count,
    )
}

/** The selected shop (or the nearby shops while nothing is selected) as cards that swipe around in a loop; swiping selects the shop. */
@Composable
internal fun MapShopCarousel(state: MapState, dismissInput: () -> Unit, onAction: (MapAction) -> Unit, modifier: Modifier = Modifier) {
    val selected = state.selectedShop
    // As in prod: a shop that is already among the nearby ones keeps the order of the list; only a shop from elsewhere is put in front of ten others.
    val shops = if (selected == null || state.nearby.any { it.id == selected.id }) state.nearby else listOf(selected) + state.nearby.take(9)
    // The pager is rebuilt when the set of shops changes, so it never animates over a list that was swapped under it.
    key(shops.map { it.id }) { MapShopPager(state, shops.distinctBy { it.id }, dismissInput, onAction, modifier) }
}

@Composable
private fun MapShopPager(state: MapState, shops: List<MapMarker>, dismissInput: () -> Unit, onAction: (MapAction) -> Unit, modifier: Modifier) {
    if (shops.isEmpty()) return
    val selected = state.selectedShop
    val currentSelectedId by rememberUpdatedState(state.selectedShop?.id)
    val selectedIndex = shops.indexOfFirst { it.id == selected?.id }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = carouselStartPage(shops.size, selectedIndex), pageCount = { if (shops.size > 1) Int.MAX_VALUE else 1 })
    LaunchedEffect(selected?.id, shops.size) {
        if (selected == null || shops.isEmpty()) return@LaunchedEffect
        val current = pager.currentPage % shops.size
        if (current != selectedIndex) {
            val right = (selectedIndex - current + shops.size) % shops.size
            pager.animateScrollToPage(pager.currentPage + if (right <= shops.size / 2) right else right - shops.size)
        }
    }
    LaunchedEffect(pager, shops) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().drop(1).collect { page ->
            val shop = shops.getOrNull(page % shops.size) ?: return@collect
            if (shop.id != currentSelectedId) { dismissInput(); onAction(MapAction.SelectFromCarousel(shop.id)) }
        }
    }
    val position = stringResource(Res.string.discovery_map_carousel_position, pager.currentPage % shops.size + 1, shops.size)
    HorizontalPager(
        pager, modifier.fillMaxWidth().semantics { collectionInfo = CollectionInfo(1, shops.size); stateDescription = position },
        contentPadding = PaddingValues(horizontal = 36.dp), pageSpacing = 12.dp, userScrollEnabled = shops.size > 1, beyondViewportPageCount = 1,
    ) { page ->
        val shop = shops[page % shops.size]
        val isSelected = shop.id == selected?.id
        DiscoveryMapShopCard(
            shop, state.details.takeIf { isSelected }, isSelected && state.isLoadingDetails, state.nearbyOrigin?.let { distanceMeters(it, shop.coordinates) },
            onOpen = { if (isSelected) onAction(MapAction.OpenShop(shop.id)) else onAction(MapAction.SelectMarker(shop.id)) },
        )
    }
}

internal fun carouselStartPage(count: Int, selectedIndex: Int): Int {
    if (count <= 1) return 0
    val middle = Int.MAX_VALUE / 2
    return middle - middle % count + selectedIndex.coerceIn(0, count - 1)
}
