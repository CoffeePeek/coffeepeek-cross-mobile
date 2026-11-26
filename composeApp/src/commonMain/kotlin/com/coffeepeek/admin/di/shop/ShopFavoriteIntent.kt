package com.coffeepeek.admin.di.shop

/** A guest heart resumes the membership picker after sign-in, without writing a favorite. */
internal interface ShopFavoriteIntent {
    fun remember(shopId: String, favorite: Boolean)
}
