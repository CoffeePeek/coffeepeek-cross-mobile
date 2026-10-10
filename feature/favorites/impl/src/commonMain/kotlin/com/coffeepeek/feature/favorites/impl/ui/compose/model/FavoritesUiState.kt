package com.coffeepeek.feature.favorites.impl.ui.compose.model

import com.coffeepeek.feature.favorites.domain.model.FavoriteShop

internal data class FavoritesUiState(
    val shops: List<FavoriteShop> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val actionFailed: Boolean = false,
    val removing: Set<String> = emptySet(),
)
