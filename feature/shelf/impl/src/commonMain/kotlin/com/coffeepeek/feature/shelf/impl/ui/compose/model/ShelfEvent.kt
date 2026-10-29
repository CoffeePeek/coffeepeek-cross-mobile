package com.coffeepeek.feature.shelf.impl.ui.compose.model

/** Destinations the application owns. */
internal sealed interface ShelfEvent {
    data object OpenArchive : ShelfEvent
    data object BrowseCatalog : ShelfEvent
    data object SignIn : ShelfEvent
    data object AddBag : ShelfEvent
    data class OpenBag(val id: String) : ShelfEvent
}
