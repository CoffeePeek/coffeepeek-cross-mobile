package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput

/** Operations are bound to the account and shop that opened the form. */
interface ShopCheckInDraftStore {
    suspend fun open(initial: ShopCheckInCreateInput): Result<ShopCheckInDraftSnapshot>
    suspend fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit>
    suspend fun clear(shopSlug: String): Result<Unit>
}

data class ShopCheckInDraftSnapshot(
    val input: ShopCheckInCreateInput,
    /** An uncertain submission must not become retryable after the process is restarted. */
    val deliveryUnconfirmed: Boolean = false,
    val exists: Boolean = false,
)
