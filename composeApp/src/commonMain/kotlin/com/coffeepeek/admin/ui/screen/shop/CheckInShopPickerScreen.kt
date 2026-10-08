package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.catalog.api.ShopCard
import com.coffeepeek.admin.location.NEARBY_RADIUS_METERS
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpSearchField
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.ui.screen.map.MapScreen

@Composable
internal fun CheckInShopPickerScreen() {
    val vm: FeedViewModel = platformViewModel()
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val location = rememberPermittedUserLocation()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var showMap by rememberSaveable { mutableStateOf(false) }
    val selectShop: (String) -> Unit = { id ->
        focus.clearFocus(force = true)
        keyboard?.hide()
        Navigator.popThenNavigate(Navigator.Screen.ShopDetail(id, forCheckIn = true))
    }
    val shops = if (state.filters.nearbyOnly && location != null) {
        state.visibleShops.mapNotNull { shop ->
            distanceToShopMeters(location, shop.location)?.takeIf { it <= NEARBY_RADIUS_METERS }
                ?.let { shop to it }
        }.sortedBy { it.second }.map { it.first }
    } else state.visibleShops
    val shouldLoadMore by remember(state, shops.size, showMap) {
        derivedStateOf {
            !showMap && state.hasMore && !state.isLoading && !state.isLoadingMore &&
                !state.isRefreshing && state.error == null &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= shops.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.currentPage) { if (shouldLoadMore) vm.loadMore() }
    LaunchedEffect(state.query, state.filters.nearbyOnly) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(CpDimens.spacing4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Выберите кофейню", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = Navigator::popBack) {
                Icon(CpIcons.Close, contentDescription = "Закрыть выбор кофейни")
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing4),
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            if (location != null) FilterChip(
                selected = state.filters.nearbyOnly && !showMap,
                onClick = { showMap = false; vm.toggleNearbyOnly() },
                label = { Text("Рядом со мной") },
                leadingIcon = { Icon(CpIcons.Location, null, Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = showMap,
                onClick = { focus.clearFocus(force = true); keyboard?.hide(); showMap = !showMap },
                label = { Text(if (showMap) "Списком" else "На карте") },
                leadingIcon = { Icon(if (showMap) CpIcons.Search else CpIcons.Map, null, Modifier.size(18.dp)) },
            )
        }
        if (showMap) {
            MapScreen(modifier = Modifier.weight(1f), onOpenShop = selectShop)
        } else {
            CpSearchField(
                value = state.query, onValueChange = vm::onQueryChange, placeholder = "Поиск кофеен",
                modifier = Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
            )
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            ) {
                if (state.isLoading && shops.isEmpty()) item { CoffeePeekLoader() }
                items(shops, key = { it.id }) { shop ->
                    ShopCard(
                        shop = shop, distance = formatDistance(distanceToShopMeters(location, shop.location)),
                        onClick = { selectShop(shop.id) }, onToggleFavorite = { vm.toggleFavorite(shop) },
                        showCatalogDetails = false,
                    )
                }
                when {
                    state.error != null -> item {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = vm::refresh) { Text("Попробовать снова") }
                    }
                    shops.isEmpty() && !state.isLoading && !state.isLoadingMore && !state.hasMore -> item {
                        Text("Кофейни не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (state.isLoadingMore) item { CoffeePeekLoader() }
            }
        }
    }
}
