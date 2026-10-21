package com.coffeepeek.feature.coffee.impl.ui.compose.model

internal sealed interface CoffeeListAction {
    data object Initialize : CoffeeListAction
    data class QueryChanged(val query: String) : CoffeeListAction
    data object Refresh : CoffeeListAction
    data object LoadMore : CoffeeListAction
    data object RetryFilters : CoffeeListAction
    data object OpenFilters : CoffeeListAction
    data object CancelFilters : CoffeeListAction
    data object ResetFilterDraft : CoffeeListAction
    data class FilterToggled(val group: String, val option: String) : CoffeeListAction
    data class AvailableOnlyChanged(val selected: Boolean) : CoffeeListAction
    data object ApplyFilters : CoffeeListAction
    data class OpenCoffee(val slug: String) : CoffeeListAction
}
