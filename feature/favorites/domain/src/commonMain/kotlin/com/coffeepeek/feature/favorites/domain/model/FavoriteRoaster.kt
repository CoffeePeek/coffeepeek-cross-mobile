package com.coffeepeek.feature.favorites.domain.model

/** Saved catalogue identity; a missing public slug must not become an invented route. */
data class FavoriteRoaster(
    val id: String,
    val title: String,
    val publicSlug: String? = null,
    val photoUrl: String? = null,
    val coffeeShopsCount: Int = 0,
    val coffeeProductsCount: Int = 0,
)
