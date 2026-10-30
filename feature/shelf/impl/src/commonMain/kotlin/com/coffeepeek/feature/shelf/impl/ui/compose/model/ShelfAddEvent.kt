package com.coffeepeek.feature.shelf.impl.ui.compose.model

internal sealed interface ShelfAddEvent {
    data object Saved : ShelfAddEvent
    data object Close : ShelfAddEvent
}
