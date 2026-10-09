package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput

/** App composition adapts the existing one-shop, process-lifetime draft store. */
interface ShopCheckInDraftStore {
    fun open(initial: ShopCheckInCreateInput): Result<ShopCheckInDraftSnapshot>
    fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit>
    fun clear(shopSlug: String): Result<Unit>
}

data class ShopCheckInDraftSnapshot(
    val input: ShopCheckInCreateInput,
    /** Retained across sheet recreation; an uncertain write must not silently become retryable. */
    val deliveryUnconfirmed: Boolean = false,
)
