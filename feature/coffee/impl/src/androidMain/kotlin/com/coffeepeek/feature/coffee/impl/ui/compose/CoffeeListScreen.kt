package com.coffeepeek.feature.coffee.impl.ui.compose

import com.coffeepeek.feature.coffee.api.CoffeeExposure
import com.coffeepeek.feature.coffee.api.CoffeeScreenAnalytics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.impl.R
import com.coffeepeek.feature.coffee.impl.ui.CoffeeListViewModel
import com.coffeepeek.feature.coffee.impl.ui.compose.component.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun CoffeeListScreen(viewModel: CoffeeListViewModel, imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit,
    bottomClearance: Dp, modifier: Modifier, screen: CoffeeScreenAnalytics, loadError: (@Composable (onRetry: () -> Unit) -> Unit)?,
    scrollToTopRequests: Flow<Unit>, animateScrollToTop: Boolean,
    onEvent: (CoffeeListEvent) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    CoffeeListTabReselection(listState, scrollToTopRequests, animateScrollToTop)
    CoffeeListScreenContent(state, viewModel::onAction, imageContent, modifier, bottomClearance, screen, loadError, listState)
}

@Composable
private fun CoffeeListTabReselection(listState: LazyListState, requests: Flow<Unit>, animate: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentAnimate by rememberUpdatedState(animate)
    LaunchedEffect(listState, requests, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            requests.collectLatest {
                if (currentAnimate) listState.animateScrollToItem(0) else listState.scrollToItem(0)
            }
        }
    }
}

@Composable
internal fun CoffeeListScreenContent(state: CoffeeListState, onAction: (CoffeeListAction) -> Unit,
    imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit, modifier: Modifier = Modifier, bottomClearance: Dp = 0.dp,
    screen: CoffeeScreenAnalytics = CoffeeScreenAnalytics.None, loadError: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState()) {
    val tasteNames = remember(state.filterGroups) {
        state.filterGroups.firstOrNull { it.code == "taste" }?.options.orEmpty().associate { it.code to it.name }
    }
    CoffeeListPagingEffects(state, listState, onAction)
    if (state.filterDraft != null) screen.Overlay { CoffeeFiltersSheet(state, onAction) }
    Scaffold(modifier, contentWindowInsets = WindowInsets(0, 0, 0, 0), containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3)) {
                CoffeeSearchHeader(state.query, state.filters.activeCount, { onAction(CoffeeListAction.QueryChanged(it)) },
                    { onAction(CoffeeListAction.OpenFilters) }, screen)
            }
        }) { padding ->
        CoffeePeekPullToRefresh(listState, state.isRefreshing, { onAction(CoffeeListAction.Refresh) },
            stringResource(R.string.coffee_refresh), stringResource(R.string.coffee_loading),
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().clipToBounds()) { scrollModifier ->
            LazyColumn(state = listState, modifier = screen.exposure(scrollModifier.fillMaxSize().testTag("coffee-list"), CoffeeExposure.Catalog),
                contentPadding = PaddingValues(start = CoffeePeekDimens.spacing4, top = CoffeePeekDimens.spacing4,
                    end = CoffeePeekDimens.spacing4, bottom = bottomClearance + 88.dp),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
                if (state.isLoading && state.items.isEmpty()) item(key = "loading") { LoadingRow() }
                items(state.items, key = { it.slug }) { coffee ->
                    CoffeeCard(coffee, tasteNames, { onAction(CoffeeListAction.OpenCoffee(coffee.slug)) }, imageContent)
                }
                if (state.isLoadingMore) item(key = "more-loading") { LoadingRow() }
                if (state.loadFailed || state.loadMoreFailed) item(key = "error") {
                    val retry = { onAction(if (state.loadMoreFailed) CoffeeListAction.LoadMore else CoffeeListAction.Refresh) }
                    if (loadError != null) loadError(retry) else CoffeeLoadError(retry)
                } else if (state.items.isEmpty() && !state.isLoading) item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.coffee_empty), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.coffee_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoffeeListPagingEffects(state: CoffeeListState, listState: LazyListState, onAction: (CoffeeListAction) -> Unit) {
    val currentAction by rememberUpdatedState(onAction)
    LaunchedEffect(state.query, state.filters) { listState.scrollToItem(0) }
    LaunchedEffect(state.items.size, state.hasMore, state.isLoading, state.isRefreshing, state.isLoadingMore, state.loadFailed, state.loadMoreFailed) {
        if (state.items.isNotEmpty() && state.hasMore && !state.isLoading && !state.isRefreshing && !state.isLoadingMore && !state.loadFailed && !state.loadMoreFailed) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { index ->
                if (index >= state.items.size - 4) currentAction(CoffeeListAction.LoadMore)
            }
        }
    }
}

@Composable private fun LoadingRow() {
    Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), contentAlignment = Alignment.Center) {
        CoffeePeekLoader(stringResource(R.string.coffee_loading))
    }
}

@PreviewLightDark @Composable private fun CoffeeListPreview() = CoffeePeekTheme {
    val items = List(4) { previewCoffee().copy(slug = "preview-coffee-$it") }
    CoffeeListScreenContent(CoffeeListState(items = items, isLoading = false), {},
        { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) })
}
@PreviewLightDark @Composable private fun CoffeeListEmptyPreview() = CoffeePeekTheme {
    CoffeeListScreenContent(CoffeeListState(isLoading = false), {}, { _, _, _, _ -> })
}
@PreviewLightDark @Composable private fun CoffeeListErrorPreview() = CoffeePeekTheme {
    CoffeeListScreenContent(CoffeeListState(isLoading = false, loadFailed = true), {}, { _, _, _, _ -> })
}
