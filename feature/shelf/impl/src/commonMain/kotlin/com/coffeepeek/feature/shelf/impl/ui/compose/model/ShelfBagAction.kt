package com.coffeepeek.feature.shelf.impl.ui.compose.model

internal sealed interface ShelfBagAction {
    data object Back : ShelfBagAction
    data object RetryBrews : ShelfBagAction
    data object OpenCoffee : ShelfBagAction
    data object Brew : ShelfBagAction
    data class BrewClosed(val saved: Boolean) : ShelfBagAction
    data object StartFinish : ShelfBagAction
    data object CancelFinish : ShelfBagAction
    data class Finish(val rating: Int?, val buyAgain: Boolean?) : ShelfBagAction
    data object Restore : ShelfBagAction
    data object RequestDelete : ShelfBagAction
    data object CancelDelete : ShelfBagAction
    data object Delete : ShelfBagAction
    data object DismissFailure : ShelfBagAction
}
