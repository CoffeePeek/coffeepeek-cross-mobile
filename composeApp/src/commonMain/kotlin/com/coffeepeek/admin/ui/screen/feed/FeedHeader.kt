package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.SearchHeader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FeedHeader(state: FeedUiState, layout: FeedLayoutState, actions: FeedScreenActions,
    motion: FeedMapMotion, hasMap: Boolean, showNearby: Boolean) {
    Box(Modifier.fillMaxWidth().then(if (layout.mapExpanded) Modifier.clearAndSetSemantics {} else Modifier)
        .graphicsLayer {
            alpha = 1f - motion.expansion.value
            translationY = -size.height * motion.expansion.value
        }) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Column(Modifier.fillMaxWidth().padding(top = CpDimens.spacing3, bottom = CpDimens.spacing2)) {
                FeedSearchHeader(state, actions, hasMap)
                Spacer(Modifier.height(CpDimens.spacing2))
                FeedHeaderFilters(state.filters, actions, showNearby)
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun FeedSearchHeader(state: FeedUiState, actions: FeedScreenActions, hasMap: Boolean) {
    Row(Modifier.padding(horizontal = CpDimens.spacing4), verticalAlignment = Alignment.CenterVertically) {
        SearchHeader(modifier = Modifier.weight(1f), query = state.query, onQueryChange = actions.onQueryChange,
            roastersSelected = false, onSelectRoasters = { if (it) actions.onSelectRoasters() },
            filterCount = state.activeFilterCount, onFilters = actions.onFilters, showCategories = !hasMap,
            onSearchFocus = { if (hasMap) actions.onActivateSearch() },
            onCancelSearch = actions.onCancelSearch.takeIf { hasMap && !state.showDiscovery })
    }
}

@Composable
private fun FeedHeaderFilters(filters: FeedFiltersUi, actions: FeedScreenActions, showNearby: Boolean) {
    FeedQuickFilterBar(nearbyOnly = filters.nearbyOnly, showNearby = showNearby,
        openOnly = filters.openOnly, newOnly = filters.newOnly, visitedOnly = filters.visitedOnly,
        favoritesOnly = filters.favoritesOnly, onToggleOpen = actions.onToggleOpen, onToggleNew = actions.onToggleNew,
        onToggleVisited = actions.onToggleVisited, onToggleFavorites = actions.onToggleFavorites,
        onToggleNearby = actions.onToggleNearby, coffeeFocusId = filters.coffeeFocus,
        onCoffeeFocusChange = actions.onCoffeeFocusChange)
}

@Composable
private fun FeedHeaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FeedHeader(FeedUiState(query = "Coffee", isSearchActive = true), rememberFeedLayout(), FeedScreenActions(),
        FeedMapMotion(androidx.compose.runtime.rememberUpdatedState(0f), androidx.compose.runtime.rememberUpdatedState(0f)), true, true)
}

@Preview @Composable private fun FeedHeaderLightPreview() = FeedHeaderPreview(false)
@Preview @Composable private fun FeedHeaderDarkPreview() = FeedHeaderPreview(true)
