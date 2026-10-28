package com.coffeepeek.feature.shelf.impl.ui.compose.model

internal sealed interface ShelfAction {
    data object Refresh : ShelfAction
    data class OpenBag(val id: String) : ShelfAction
    data object CloseBag : ShelfAction
    data class Move(val id: String, val toIndex: Int) : ShelfAction
    data class StartFinish(val id: String) : ShelfAction
    data object CancelFinish : ShelfAction
    data class Finish(val id: String, val rating: Int?, val buyAgain: Boolean?) : ShelfAction
    data class UndoFinish(val id: String, val position: Int) : ShelfAction
    data class RequestDelete(val id: String) : ShelfAction
    data object CancelDelete : ShelfAction
    data class Delete(val id: String) : ShelfAction
    data object DismissMessage : ShelfAction
    data object OpenArchive : ShelfAction
    data object BrowseCatalog : ShelfAction
    data object SignIn : ShelfAction
    data object AddBag : ShelfAction
}
