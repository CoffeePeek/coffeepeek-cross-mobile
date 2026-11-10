package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote

/** Check-in votes use the visit contract, never the previous review endpoint. */
interface ShopCheckInVoteRepository {
    suspend fun setHelpful(checkInId: String, helpful: Boolean): Result<ShopHelpfulVote>
}
