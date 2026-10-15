package com.coffeepeek.feature.favorites.impl.ui.compose.model

import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster

internal data class FavoritesUiState(
    val shops: List<FavoriteShop> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val actionFailed: Boolean = false,
    val removing: Set<String> = emptySet(),
    val roasters: List<FavoriteRoaster> = emptyList(),
    val areRoastersLoading: Boolean = false,
    val roastersLoadFailed: Boolean = false,
    val removingRoasters: Set<String> = emptySet(),
)
