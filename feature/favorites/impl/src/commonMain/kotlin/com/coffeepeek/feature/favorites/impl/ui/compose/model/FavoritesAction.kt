package com.coffeepeek.feature.favorites.impl.ui.compose.model

/** Every user intent enters the ViewModel through one direction. */
internal sealed interface FavoritesAction {
    data object Retry : FavoritesAction
    data class Remove(val shopId: String) : FavoritesAction
    data class OpenShop(val shopId: String) : FavoritesAction
    data object Back : FavoritesAction
}
