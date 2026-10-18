package com.coffeepeek.admin.ui.screen.roaster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.analyticsExposure
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.feature.catalog.api.RoasterCard
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CpSnackbarHost
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.LoadErrorContent
import com.coffeepeek.admin.ui.component.SearchHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoasterListScreen(
    onCancel: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedRoasterIds: Set<String> = emptySet(),
    favoritesOnly: Boolean = false,
    vm: RoasterListViewModel = platformViewModel(),
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val visible = state.visibleItems(query, selectedRoasterIds, favoritesOnly)
    LaunchedEffect(query, selectedRoasterIds, favoritesOnly, state.selectedTagIds, state.isLoading, state.error) {
        kotlinx.coroutines.delay(350)
        vm.resultsShown(query, selectedRoasterIds, favoritesOnly)
    }
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
        snackbarHost = { CpSnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3)) {
                SearchHeader(
                    query = query, onQueryChange = onQueryChange,
                    analyticsCatalog = "roasters", analyticsScreen = "roasters",
                    roastersSelected = true, onSelectRoasters = {}, showCategories = false,
                    onCancelSearch = {
                        focusManager.clearFocus(force = true)
                        keyboard?.hide()
                        onCancel()
                    },
                )
                RoasterTagFilters(state, vm::toggleTag, vm::clearTags)
            }
        },
    ) { padding ->
        CoffeePeekPullToRefresh(
            listState = listState, isRefreshing = state.isLoading,
            onRefresh = vm::refresh, modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
        ) { scrollModifier ->
            LazyColumn(
                state = listState, modifier = scrollModifier.fillMaxSize().analyticsExposure("roaster_catalog", "catalog", "roasters"),
                contentPadding = PaddingValues(start = CpDimens.spacing4, top = CpDimens.spacing2, end = CpDimens.spacing4, bottom = clearance + 88.dp),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            ) {
                itemsIndexed(visible, key = { index, item -> item.catalog.id.ifBlank { "unaddressed-roaster:$index" } }) { index, item ->
                    RoasterCard(
                        roaster = item.catalog,
                        details = item.details,
                        onClick = { item.routeId?.let { vm.openedRoaster(index); Navigator.navigate(Navigator.Screen.RoasterDetail(it)) } },
                        isFavorite = item.catalog.roasterFavoriteId in state.favoriteIds,
                        isFavoriteLoading = item.catalog.roasterFavoriteId in state.savingFavoriteIds,
                        onToggleFavorite = { vm.toggleFavorite(item) },
                    )
                }
                if (state.isLoading) item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                }
                if (state.error != null) item {
                    LoadErrorContent(onRetry = vm::refresh)
                } else if (visible.isEmpty() && !state.isLoading) item {
                    Text("Обжарщики не найдены", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun RoasterPreview(
    query: String = "",
    selectedRoasterIds: Set<String> = emptySet(),
    favoritesOnly: Boolean = false,
    vm: RoasterListViewModel = platformViewModel(),
) {
    val state by vm.state.collectAsState()
    val visible = state.visibleItems(query, selectedRoasterIds, favoritesOnly)
    LaunchedEffect(query, selectedRoasterIds, favoritesOnly, state.selectedTagIds, state.isLoading, state.error) {
        kotlinx.coroutines.delay(350)
        vm.resultsShown(query, selectedRoasterIds, favoritesOnly)
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearActionMessage()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        RoasterTagFilters(state, vm::toggleTag, vm::clearTags)
        BoxWithConstraints(Modifier.fillMaxWidth().analyticsExposure("roaster_catalog", "discovery", "discovery")) {
            val cardWidth = (maxWidth * 0.88f).coerceIn(280.dp, 360.dp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                itemsIndexed(visible.take(8), key = { index, item -> item.catalog.id.ifBlank { "unaddressed-roaster:$index" } }) { index, item ->
                    RoasterCard(
                        roaster = item.catalog,
                        details = item.details,
                        onClick = { item.routeId?.let { vm.openedRoaster(index); Navigator.navigate(Navigator.Screen.RoasterDetail(it)) } },
                        isFavorite = item.catalog.roasterFavoriteId in state.favoriteIds,
                        isFavoriteLoading = item.catalog.roasterFavoriteId in state.savingFavoriteIds,
                        onToggleFavorite = { vm.toggleFavorite(item) },
                        modifier = Modifier.width(cardWidth),
                    )
                }
            }
            CpSnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
        if (state.items.isEmpty() && state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
        } else if (state.error != null) {
            LoadErrorContent(onRetry = vm::refresh)
        } else if (visible.isEmpty() && !state.isLoading) {
            Text("Обжарщики не найдены", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoasterTagFilters(state: RoasterListUiState, onToggle: (String) -> Unit, onClear: () -> Unit) {
    if (state.availableTags.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        item {
            FilterChip(selected = state.selectedTagIds.isEmpty(), onClick = onClear, label = { Text("Все") })
        }
        items(state.availableTags, key = { it.slug }) { tag ->
            FilterChip(selected = tag.slug in state.selectedTagIds, onClick = { onToggle(tag.slug) }, label = { Text(tag.name) })
        }
    }
}
