package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopDetails

internal data class ShopDetailState(
    val details: ShopDetails? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    val todayDayOfWeek: Int = 0,
    val scheduleExpanded: Boolean = false,
    val featuresExpanded: Boolean = false,
    val pendingCheckInVoteId: String? = null,
    val isFavorite: Boolean = false,
    val favoriteAvailable: Boolean = false,
    val isFavoriteLoading: Boolean = false,
)
