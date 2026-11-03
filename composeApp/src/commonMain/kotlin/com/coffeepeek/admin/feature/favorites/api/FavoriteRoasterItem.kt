package com.coffeepeek.admin.feature.favorites.api

/** A roaster as the favorites bridge between the roaster and favorites features carries it. */
data class PublicAddress(
    val slug: String,
    val canonicalPath: String,
    val revision: Int,
    val isAlias: Boolean,
)

data class CatalogItem(
    val id: String,
    val name: String,
    val slug: String = "",
    val photoUrl: String? = null,
    val address: PublicAddress? = null,
    val coffeeShopsCount: Int = 0,
    val coffeeProductsCount: Int = 0,
    val availableCoffeeProducts: Int = 0,
    val tags: List<CatalogItem> = emptyList(),
    val description: String? = null,
    val sortOrder: Int = 0,
)
