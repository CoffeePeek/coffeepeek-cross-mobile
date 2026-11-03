package com.coffeepeek.feature.collections.domain.model

enum class CollectionPlaceKind { CoffeeShop, Roaster }

data class CollectionPlace(
    val kind: CollectionPlaceKind,
    val slug: String,
    val title: String,
    val photoUrl: String? = null,
    val city: String? = null,
    val address: String? = null,
    val rating: Double? = null,
    val checkInCount: Int? = null,
) {
    val key: String get() = "${kind.name}:$slug"
}
