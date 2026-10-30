package com.coffeepeek.feature.shelf.impl.ui.compose.model

internal sealed interface ShelfArchiveEvent {
    data object Back : ShelfArchiveEvent
    data class OpenBag(val id: String) : ShelfArchiveEvent
}
