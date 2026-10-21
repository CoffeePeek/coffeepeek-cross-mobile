package com.coffeepeek.feature.shop.api

/** Caller route identity and the API's public address slug are deliberately separate. */
data class ShopDetailContext(
    val shopId: String,
    val shopSlug: String,
    val shopName: String,
)
