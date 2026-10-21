package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto

/** Presentation-only draft. The platform bridge keeps text/ratings persistent and photos in memory. */
data class ShopReviewDraftSnapshot(
    val header: String,
    val comment: String,
    val rating: ShopRating,
    val photos: List<ShopReviewPhoto>,
)

/** One store instance is scoped to one new-review or edit-review key by application composition. */
interface ShopReviewDraftStore {
    suspend fun load(): Result<ShopReviewDraftSnapshot?>
    fun save(draft: ShopReviewDraftSnapshot): Result<Unit>
    suspend fun clear(): Result<Unit>
}
