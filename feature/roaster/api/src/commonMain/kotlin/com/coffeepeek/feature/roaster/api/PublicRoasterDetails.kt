package com.coffeepeek.feature.roaster.api

/** Read-only public card enrichment over the same catalog/cache as the roaster screens. */
interface PublicRoasterDetails {
    suspend fun summaries(): Result<List<PublicRoasterSummary>>
    suspend fun load(slug: String): Result<PublicRoasterDescription>
}

data class PublicRoasterSummary(
    val slug: String, val photoUrl: String?, val tags: List<String>,
    val coffeeShopsCount: Int, val coffeeProductsCount: Int, val availableCoffeeProducts: Int,
)

data class PublicRoasterDescription(val photoUrl: String?, val about: String?)
