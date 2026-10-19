package com.coffeepeek.feature.roaster.impl.ui.compose

import com.coffeepeek.feature.roaster.api.RoasterImage
import com.coffeepeek.feature.roaster.api.RoasterLoadError
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.api.RoasterExposure
import com.coffeepeek.feature.roaster.api.RoasterScreenAnalytics
import com.coffeepeek.feature.roaster.domain.model.RoasterTag
import com.coffeepeek.feature.roaster.impl.R
import com.coffeepeek.feature.roaster.impl.ui.RoasterListViewModel
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterEmptyText
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterListItemCard
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterLoadingRow
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterTagFilters
import com.coffeepeek.feature.roaster.impl.ui.compose.component.text
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListItem
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListState
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import com.coffeepeek.feature.roaster.domain.model.RoasterAddress

/** Lifecycle adapter; the search header and criteria (query, Discovery's selection) stay with the caller. */
@Composable
internal fun RoasterListScreen(
    viewModel: RoasterListViewModel,
    query: String,
    selectedRoasters: Set<String>,
    favoritesOnly: Boolean,
    image: RoasterImage,
    loadError: RoasterLoadError,
    screen: RoasterScreenAnalytics,
    onFavoriteFeedback: () -> Unit,
    bottomClearance: Dp,
    modifier: Modifier,
    header: @Composable ColumnScope.() -> Unit,
    onEvent: (RoasterListEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    LaunchedEffect(query) { viewModel.onAction(RoasterListAction.QueryChanged(query)) }
    LaunchedEffect(selectedRoasters) { viewModel.onAction(RoasterListAction.SelectedRoastersChanged(selectedRoasters)) }
    LaunchedEffect(favoritesOnly) { viewModel.onAction(RoasterListAction.FavoritesOnlyChanged(favoritesOnly)) }
    RoasterListScreenContent(state, viewModel::onAction, image, loadError, screen, onFavoriteFeedback, modifier, bottomClearance, header)
}

@Composable
internal fun RoasterListScreenContent(
    state: RoasterListState,
    onAction: (RoasterListAction) -> Unit,
    image: RoasterImage,
    loadError: RoasterLoadError,
    screen: RoasterScreenAnalytics = RoasterScreenAnalytics.None,
    onFavoriteFeedback: () -> Unit = {},
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 0.dp,
    header: @Composable ColumnScope.() -> Unit = {},
) {
    val listState = rememberLazyListState()
    val visible = state.visibleItems
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.text()
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message); onAction(RoasterListAction.DismissMessage) }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3)) {
                header()
                RoasterTagFilters(state.availableTags, state.selectedTags,
                    { onAction(RoasterListAction.ToggleTag(it)) }, { onAction(RoasterListAction.ClearTags) })
            }
        },
    ) { padding ->
        CoffeePeekPullToRefresh(listState, state.isLoading, { onAction(RoasterListAction.Refresh) },
            stringResource(R.string.roaster_refresh), stringResource(R.string.roaster_loading),
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()) { scrollModifier ->
            LazyColumn(
                state = listState,
                modifier = screen.exposure(scrollModifier.fillMaxSize(), RoasterExposure.Catalog),
                contentPadding = PaddingValues(start = CoffeePeekDimens.spacing4, top = CoffeePeekDimens.spacing2,
                    end = CoffeePeekDimens.spacing4, bottom = bottomClearance + 88.dp),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
            ) {
                itemsIndexed(visible, key = { index, item -> item.slug ?: "unaddressed-roaster:$index" }) { _, item ->
                    RoasterListItemCard(item, state.isLoading, item.favoriteKey in state.favoriteKeys, onAction, onFavoriteFeedback, image, screen)
                }
                if (state.isLoading) item { RoasterLoadingRow() }
                if (state.loadFailed) item { loadError { onAction(RoasterListAction.Refresh) } }
                else if (visible.isEmpty() && !state.isLoading) item { RoasterEmptyText(Modifier.padding(24.dp)) }
            }
        }
    }
}

internal fun previewRoasterItem(slug: String, name: String = "Roaster $slug", tags: List<RoasterTag> = emptyList()) =
    RoasterListItem(RoasterSummary(RoasterAddress(slug, "/roasters/$slug", 1, false), name, null, tags, 7, 21, 19))

@PreviewLightDark @Composable private fun RoasterListPreview() = CoffeePeekTheme {
    RoasterListScreenContent(
        RoasterListState(items = listOf(previewRoasterItem("a", tags = listOf(RoasterTag("light", "Светлая", null, 1))),
            previewRoasterItem("b")), isLoading = false, favoriteKeys = setOf("a")),
        {}, { _, _, _, _, _ -> }, { onRetry -> androidx.compose.material3.TextButton(onRetry) { androidx.compose.material3.Text("Повторить") } })
}

@PreviewLightDark @Composable private fun RoasterListEmptyPreview() = CoffeePeekTheme {
    RoasterListScreenContent(RoasterListState(isLoading = false), {}, { _, _, _, _, _ -> }, {})
}

@PreviewLightDark @Composable private fun RoasterListErrorPreview() = CoffeePeekTheme {
    RoasterListScreenContent(RoasterListState(isLoading = false, loadFailed = true), {}, { _, _, _, _, _ -> },
        { onRetry -> androidx.compose.material3.TextButton(onRetry) { androidx.compose.material3.Text("Попробовать снова") } })
}
