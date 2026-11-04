package com.coffeepeek.feature.shelf.impl.ui.compose.model

/** Destinations the application owns. */
internal sealed interface ShelfBagEvent {
    data object Back : ShelfBagEvent
    data class OpenCoffee(val slug: String) : ShelfBagEvent
}
