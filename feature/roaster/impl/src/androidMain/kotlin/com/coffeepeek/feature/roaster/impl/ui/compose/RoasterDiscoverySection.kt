package com.coffeepeek.feature.roaster.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
    selectedRoasters: Set<String>,
    image: RoasterImage,
    loadError: RoasterLoadError,
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
    LaunchedEffect(selectedRoasters) { viewModel.onAction(RoasterListAction.SelectedRoastersChanged(selectedRoasters)) }
    RoasterDiscoverySectionContent(state, viewModel::onAction, image, loadError, screen, onFavoriteFeedback, modifier)
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
            val cardWidth = (maxWidth * 0.88f).coerceIn(280.dp, 360.dp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                itemsIndexed(visible.take(DISCOVERY_LIMIT), key = { index, item -> item.slug ?: "unaddressed-roaster:$index" }) { _, item ->
                    RoasterListItemCard(item, state.isLoading, item.favoriteKey in state.favoriteKeys, onAction,
                        onFavoriteFeedback, image, Modifier.width(cardWidth))
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
        if (state.items.isEmpty() && state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) { RoasterLoadingRow(Modifier) }
        } else if (state.loadFailed) {
            loadError { onAction(RoasterListAction.Refresh) }
        } else if (visible.isEmpty() && !state.isLoading) {
            RoasterEmptyText()
        }
    }
}

@PreviewLightDark @Composable private fun RoasterDiscoverySectionPreview() = CoffeePeekTheme {
    RoasterDiscoverySectionContent(
        RoasterListState(items = listOf(previewRoasterItem("a", tags = listOf(RoasterTag("light", "Светлая", null, 1))),
            previewRoasterItem("b"), previewRoasterItem("c")), isLoading = false, favoriteKeys = setOf("b")),
        {}, { _, _, _, _, _ -> }, {})
}

@PreviewLightDark @Composable private fun RoasterDiscoverySectionEmptyPreview() = CoffeePeekTheme {
    RoasterDiscoverySectionContent(RoasterListState(isLoading = false), {}, { _, _, _, _, _ -> }, {})
}
