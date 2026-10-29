package com.coffeepeek.feature.shop.api

/** Read-only card enrichment; no favorite state or personal check-ins cross the feature boundary. */
interface PublicShopDetails {
    suspend fun load(slug: String): Result<PublicShopSummary>
}

data class PublicShopSummary(
    val photoUrl: String?, val description: String?, val address: String?, val rating: Double?,
    val reviewCount: Int, val isOpen: Boolean, val tags: List<String>,
)
