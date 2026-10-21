package com.coffeepeek.feature.favorites.domain.model

/** A saved drink has a removal key but no contracted detail destination. */
data class FavoriteDrink(val id: String, val title: String, val photoUrl: String?, val canRemove: Boolean)
