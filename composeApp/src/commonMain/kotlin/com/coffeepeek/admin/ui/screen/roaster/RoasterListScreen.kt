package com.coffeepeek.admin.ui.screen.roaster

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.component.SearchHeader
import com.coffeepeek.admin.ui.component.FavoriteButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoasterListScreen(onCancel: () -> Unit, vm: RoasterListViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val visible = state.visibleItems
    val clearance = LocalFloatingNavClearance.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearActionMessage()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3)) {
                SearchHeader(
                    query = state.query, onQueryChange = vm::onQueryChange,
                    roastersSelected = true, onSelectRoasters = {}, showCategories = false,
                    onCancelSearch = {
                        focusManager.clearFocus(force = true)
                        keyboard?.hide()
                        vm.onQueryChange("")
                        onCancel()
                    },
                )
            }
        },
    ) { padding ->
        CoffeePeekPullToRefresh(
            listState = listState, isRefreshing = state.isLoading,
            onRefresh = vm::refresh, modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
        ) { scrollModifier ->
            LazyColumn(
                state = listState, modifier = scrollModifier.fillMaxSize(),
                contentPadding = PaddingValues(start = CpDimens.spacing4, top = CpDimens.spacing2, end = CpDimens.spacing4, bottom = clearance + CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            ) {
                items(visible, key = { it.catalog.id }) { item ->
                    RoasterCard(
                        item = item,
                        isFavorite = item.catalog.roasterFavoriteId in state.favoriteIds,
                        isFavoriteLoading = item.catalog.roasterFavoriteId in state.savingFavoriteIds,
                        onToggleFavorite = { vm.toggleFavorite(item) },
                    )
                }
                if (state.isLoading) item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                }
                if (state.error != null) item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error.orEmpty())
                        TextButton(onClick = vm::refresh) { Text("Попробовать снова") }
                    }
                } else if (visible.isEmpty() && !state.isLoading) item {
                    Text("Обжарщики не найдены", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RoasterCard(
    item: RoasterListItem,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .clickable(enabled = item.routeId != null) {
                item.routeId?.let { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) }
            },
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(CpDimens.spacing3), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                Box(Modifier.size(88.dp).clip(RoundedCornerShape(CpDimens.radiusMd)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    val photo = item.catalog.photoUrl ?: item.details?.photos?.firstOrNull()?.fullUrl
                    if (!photo.isNullOrBlank()) CoffeeShopImage(
                        imageUrl = photo, contentDescription = item.catalog.name,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                    ) else Icon(CpIcons.Factory, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.catalog.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite, enabled = !isFavoriteLoading)
                    }
                    // Temporary presentation tags until the roaster catalog supplies them.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Светлая", "Декаф").forEach { tag ->
                            Surface(shape = RoundedCornerShape(percent = 50), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(CpIcons.Coffee, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    item.details?.let { roasterShopCountLabel(it.shops.size) } ?: "Использование пока не указано",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun RoasterPreview(vm: RoasterListViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearActionMessage()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Box {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                items(state.items.take(8), key = { it.catalog.id }) { item ->
                    RoasterCard(
                        item = item,
                        isFavorite = item.catalog.roasterFavoriteId in state.favoriteIds,
                        isFavoriteLoading = item.catalog.roasterFavoriteId in state.savingFavoriteIds,
                        onToggleFavorite = { vm.toggleFavorite(item) },
                        modifier = Modifier.width(320.dp),
                    )
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
        if (state.items.isEmpty() && state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
        } else if (state.error != null) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = vm::refresh) { Text("Попробовать снова") }
        } else if (state.items.isEmpty()) {
            Text("Обжарщики не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun roasterShopCountLabel(count: Int): String {
    val noun = when {
        count % 100 in 11..14 -> "кофеен используют"
        count % 10 == 1 -> "кофейня использует"
        count % 10 in 2..4 -> "кофейни используют"
        else -> "кофеен используют"
    }
    return "$count $noun это зерно"
}
