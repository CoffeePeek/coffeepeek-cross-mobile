package com.coffeepeek.feature.shelf.impl.ui.compose.model

internal sealed interface ShelfArchiveAction {
    data class QueryChanged(val query: String) : ShelfArchiveAction
    data object Retry : ShelfArchiveAction
    data object LoadMore : ShelfArchiveAction
    data class OpenBag(val id: String) : ShelfArchiveAction
    data object CloseBag : ShelfArchiveAction
    data class Restore(val id: String) : ShelfArchiveAction
    data class RequestDelete(val id: String) : ShelfArchiveAction
    data object CancelDelete : ShelfArchiveAction
    data class Delete(val id: String) : ShelfArchiveAction
    data object DismissMessage : ShelfArchiveAction
    data object Back : ShelfArchiveAction
}
