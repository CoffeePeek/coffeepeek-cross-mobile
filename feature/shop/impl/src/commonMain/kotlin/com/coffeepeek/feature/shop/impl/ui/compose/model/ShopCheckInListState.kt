package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInRatingSummary

internal data class ShopCheckInListState(
    val sessionGeneration: Int = 0,
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val hasLoadError: Boolean = false,
    val hasVoteError: Boolean = false,
    val shopTitle: String = "",
    val overallRating: Double? = null,
    val checkInCount: Int = 0,
    val ratingSampleSize: Int = 0,
    val ratingSummary: ShopCheckInRatingSummary? = null,
    // Guests retain only the first public card; hidden content never enters rendering state.
    val checkIns: List<ShopCheckIn> = emptyList(),
    val hiddenCheckInCount: Int = 0,
    val ownCheckInIds: Set<String> = emptySet(),
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    val pendingVoteId: String? = null,
) {
    fun owns(checkIn: ShopCheckIn): Boolean = checkIn.id in ownCheckInIds ||
        currentUserId?.takeIf(String::isNotBlank)?.let { it == checkIn.userId } == true
}
