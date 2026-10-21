package com.coffeepeek.feature.roaster.domain.model

data class RoasterTag(val slug: String, val name: String, val description: String?, val sortOrder: Int)

data class RoasterSummary(
    val address: RoasterAddress?,
    val name: String,
    val photoUrl: String?,
    val tags: List<RoasterTag>,
    val coffeeShopsCount: Int,
    val coffeeProductsCount: Int,
    val availableCoffeeProducts: Int,
) {
    /** Route and favorite key; null for cards without a public address. */
    val slug: String? get() = address?.slug?.takeIf(String::isNotBlank)
}
