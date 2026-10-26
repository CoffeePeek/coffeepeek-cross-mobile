package com.coffeepeek.feature.roaster.impl.ui.compose

import com.coffeepeek.feature.roaster.impl.ui.compose.model.RoasterImage
import com.coffeepeek.feature.roaster.impl.ui.compose.model.RoasterLoadError
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.api.RoasterExposure
import com.coffeepeek.feature.roaster.api.RoasterScreenAnalytics
import com.coffeepeek.feature.roaster.domain.model.RoasterTag
import com.coffeepeek.feature.roaster.impl.ui.RoasterListViewModel
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterEmptyText
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterListItemCard
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterLoadingRow
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterTagFilters
import com.coffeepeek.feature.roaster.impl.ui.compose.component.text
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListState

private const val DISCOVERY_LIMIT = 8

@Composable
internal fun RoasterDiscoverySection(
    viewModel: RoasterListViewModel,
    query: String,
    selectedRoasters: Set<String>,
    favoritesOnly: Boolean,
    image: RoasterImage,
    loadError: RoasterLoadError,
    emptyContent: @Composable (Modifier) -> Unit,
    screen: RoasterScreenAnalytics,
    onFavoriteFeedback: () -> Unit,
    modifier: Modifier,
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
    RoasterDiscoverySectionContent(state, viewModel::onAction, image, loadError, screen, onFavoriteFeedback, modifier, emptyContent)
}

/** The Discovery carousel: same card width rule (88% clamped to 280–360dp) and at most eight cards as before. */
@Composable
internal fun RoasterDiscoverySectionContent(
    state: RoasterListState,
    onAction: (RoasterListAction) -> Unit,
    image: RoasterImage,
    loadError: RoasterLoadError,
    screen: RoasterScreenAnalytics = RoasterScreenAnalytics.None,
    onFavoriteFeedback: () -> Unit = {},
    modifier: Modifier = Modifier,
    emptyContent: @Composable (Modifier) -> Unit = { RoasterEmptyText(it) },
) {
    val visible = state.visibleItems
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.text()
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message); onAction(RoasterListAction.DismissMessage) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        RoasterTagFilters(state.availableTags, state.selectedTags,
            { onAction(RoasterListAction.ToggleTag(it)) }, { onAction(RoasterListAction.ClearTags) })
        BoxWithConstraints(screen.exposure(Modifier.fillMaxWidth(), RoasterExposure.Discovery)) {
            val cardWidth = ((maxWidth - CoffeePeekDimens.spacing4 * 2) * 0.88f).coerceIn(280.dp, 360.dp)
            LazyRow(Modifier.testTag("roaster-carousel"),
                contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4),
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                itemsIndexed(visible.take(DISCOVERY_LIMIT), key = { index, item -> item.slug ?: "unaddressed-roaster:$index" }) { _, item ->
                    RoasterListItemCard(item, state.isLoading, item.favoriteKey in state.favoriteKeys, onAction,
                        onFavoriteFeedback, image, screen, Modifier.width(cardWidth))
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
        if (state.items.isEmpty() && state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) { RoasterLoadingRow(Modifier) }
        } else if (state.loadFailed) {
            loadError { onAction(RoasterListAction.Refresh) }
        } else if (visible.isEmpty() && !state.isLoading) {
            emptyContent(Modifier.padding(horizontal = if (state.query.isBlank()) CoffeePeekDimens.spacing4 else 0.dp))
        }
    }
}

@Preview @Composable private fun RoasterDiscoverySectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDiscoverySectionContent(
        RoasterListState(items = listOf(previewRoasterItem("a", tags = listOf(RoasterTag("light", "Светлая", null, 1))),
            previewRoasterItem("b"), previewRoasterItem("c")), isLoading = false, favoriteKeys = setOf("b")),
        {}, { _, _, _, _, _ -> }, {})
}

@Preview @Composable private fun RoasterDiscoverySectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDiscoverySectionContent(
        RoasterListState(items = listOf(previewRoasterItem("a", tags = listOf(RoasterTag("light", "Светлая", null, 1))),
            previewRoasterItem("b"), previewRoasterItem("c")), isLoading = false, favoriteKeys = setOf("b")),
        {}, { _, _, _, _, _ -> }, {})
}

@Preview @Composable private fun RoasterDiscoverySectionEmptyLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDiscoverySectionContent(RoasterListState(isLoading = false), {}, { _, _, _, _, _ -> }, {})
}

@Preview @Composable private fun RoasterDiscoverySectionEmptyDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDiscoverySectionContent(RoasterListState(isLoading = false), {}, { _, _, _, _, _ -> }, {})
}
