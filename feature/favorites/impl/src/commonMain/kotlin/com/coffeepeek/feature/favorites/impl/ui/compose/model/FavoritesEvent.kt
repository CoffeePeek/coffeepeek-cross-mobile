package com.coffeepeek.feature.favorites.impl.ui.compose.model

/** One-off navigation effects; the application still owns the actual routes. */
internal sealed interface FavoritesEvent {
    data class OpenShop(val shopId: String) : FavoritesEvent
    data class OpenRoaster(val publicSlug: String) : FavoritesEvent
    data object Back : FavoritesEvent
}
