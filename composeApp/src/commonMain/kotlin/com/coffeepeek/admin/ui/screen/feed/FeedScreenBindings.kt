package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.CoffeeShop

internal fun feedScreenActions(vm: FeedViewModel, state: FeedUiState, dismiss: () -> Unit,
    selectRoasters: () -> Unit, refreshLocation: () -> Unit): FeedScreenActions =
    feedFilterActions(vm, state.filters, refreshLocation).copy(
        onQueryChange = vm::onQueryChange,
        onActivateSearch = vm::activateSearch,
        onCancelSearch = {
            dismiss()
            vm.cancelSearch()
        },
        onSelectRoasters = selectRoasters,
        onRefresh = vm::refresh,
        onLoadMore = vm::loadMore,
        onOpenShop = { openFeedShop(vm, dismiss, it) },
        onToggleFavorite = vm::toggleFavorite,
        onAddShop = { Navigator.navigate(Navigator.Screen.AddShop) },
    )

private fun feedFilterActions(vm: FeedViewModel, filters: FeedFiltersUi, refreshLocation: () -> Unit) = FeedScreenActions(
    onFilters = vm::toggleFilters,
    onDismissFilters = vm::closeFilters,
    onApplyFilters = vm::applyFilters,
    onToggleOpen = vm::toggleOpenOnly,
    onToggleNew = vm::toggleNewOnly,
    onToggleVisited = vm::toggleVisitedOnly,
    onToggleFavorites = vm::toggleFavoritesOnly,
    onToggleNearby = {
        vm.toggleNearbyOnly()
        if (!filters.nearbyOnly) refreshLocation()
    },
    onCoffeeFocusChange = { vm.setCoffeeFocus(it.takeUnless { id -> filters.coffeeFocus == id }) },
)

private fun openFeedShop(vm: FeedViewModel, dismiss: () -> Unit, shop: CoffeeShop) {
    dismiss()
    vm.openedShop(shop)
    Navigator.navigate(Navigator.Screen.ShopDetail(shop.id))
}
