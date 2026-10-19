package com.coffeepeek.feature.shop.api

/** Minimal identity for the application's author-block confirmation, not an auth user ID. */
data class ShopCheckInAuthor(
    val id: String?,
    val slug: String?,
    val nickname: String,
    val avatarUrl: String?,
)
