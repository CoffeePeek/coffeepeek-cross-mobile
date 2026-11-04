package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.domain.model.PublicShelf

internal data class PublicShelfProfileState(
    val shelf: PublicShelf? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val revision: Int = 0,
    val refreshing: Boolean = false,
)

internal sealed interface PublicShelfProfileAction {
    data object Refresh : PublicShelfProfileAction
    data class OpenCoffee(val id: String) : PublicShelfProfileAction
}

internal sealed interface PublicShelfProfileEvent {
    data class OpenCoffee(val slug: String) : PublicShelfProfileEvent
}
