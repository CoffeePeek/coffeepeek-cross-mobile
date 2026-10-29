package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.domain.model.CoffeeShop

internal data class FeedScreenActions(
    val onQueryChange: (String) -> Unit = {},
    val onFilters: () -> Unit = {},
    val onDismissFilters: () -> Unit = {},
    val onApplyFilters: (FeedFiltersUi) -> Unit = {},
    val onActivateSearch: () -> Unit = {},
    val onCancelSearch: () -> Unit = {},
    val onSelectRoasters: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onOpenShop: (CoffeeShop) -> Unit = {},
    val onToggleFavorite: (CoffeeShop) -> Unit = {},
    val onAddShop: () -> Unit = {},
    val onOpenMap: () -> Unit = {},
    val onToggleOpen: () -> Unit = {},
    val onToggleNew: () -> Unit = {},
    val onToggleVisited: () -> Unit = {},
    val onToggleFavorites: () -> Unit = {},
    val onToggleNearby: () -> Unit = {},
    val onCoffeeFocusChange: (String) -> Unit = {},
)
