package com.coffeepeek.feature.favorites.domain.model

enum class FavoriteKind { CoffeeShop, Roaster, Drink }

data class FavoriteAddress(val slug: String, val canonicalPath: String, val revision: Int, val isAlias: Boolean)

/** One row of the signed-in account's server favorites. */
data class FavoriteEntry(
    val kind: FavoriteKind,
    val address: FavoriteAddress? = null,
    val drinkSlug: String? = null,
    val name: String = "",
    val coverPhotoUrl: String? = null,
    val createdAtUtc: String = "",
    /** Local display identity only, for addressless entries without a contracted target id. */
    val occurrence: Int = 0,
) {
    val slug: String? get() = if (kind == FavoriteKind.Drink) drinkSlug else address?.slug
    val id: String get() = "${kind.name}:${slug ?: "$createdAtUtc:$name:$occurrence"}"
}
