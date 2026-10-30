package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Reuses the list's switches and its authentication guards on the map. */
@Composable
internal fun DiscoveryMapFilterBar(state: FeedUiState, vm: FeedViewModel) {
    FeedQuickFilterBar(
        nearbyOnly = state.filters.nearbyOnly, showNearby = state.searchOrigin != null,
        openOnly = state.filters.openOnly, newOnly = state.filters.newOnly,
        visitedOnly = state.filters.visitedOnly, favoritesOnly = state.filters.favoritesOnly,
        onToggleOpen = vm::toggleOpenOnly, onToggleNew = vm::toggleNewOnly,
        onToggleVisited = vm::toggleVisitedOnly, onToggleFavorites = vm::toggleFavoritesOnly,
        onToggleNearby = vm::toggleNearbyOnly, coffeeFocusId = state.filters.coffeeFocus,
        onCoffeeFocusChange = { vm.setCoffeeFocus(it.takeUnless { kind -> kind == state.filters.coffeeFocus }) },
    )
}

@Composable
private fun DiscoveryMapFilterBarPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FeedQuickFilterBar(nearbyOnly = false, showNearby = true, openOnly = true, newOnly = false,
        visitedOnly = false, favoritesOnly = true, onToggleOpen = {}, onToggleNew = {},
        onToggleVisited = {}, onToggleFavorites = {}, onToggleNearby = {}, coffeeFocusId = "specialty", onCoffeeFocusChange = {})
}

@Preview @Composable private fun DiscoveryMapFilterBarLightPreview() = DiscoveryMapFilterBarPreview(false)
@Preview @Composable private fun DiscoveryMapFilterBarDarkPreview() = DiscoveryMapFilterBarPreview(true)
