package com.coffeepeek.feature.shop.api

/** Read-only card enrichment; no favorite state or personal check-ins cross the feature boundary. */
interface PublicShopDetails {
    suspend fun load(slug: String): Result<PublicShopSummary>
}

data class PublicShopSummary(
    val photoUrl: String?, val description: String?, val address: String?, val rating: Double?,
    val reviewCount: Int, val isOpen: Boolean, val tags: List<String>,
    /** The public address the server knows the shop by; empty when the answer carries none. */
    val slug: String = "", val canonicalPath: String? = null, val revision: Int = 1, val isAlias: Boolean = false,
    val title: String = "", val priceRange: String? = null, val latitude: Double? = null, val longitude: Double? = null,
    val brewMethods: List<String> = emptyList(), val roasterPhotoUrls: List<String> = emptyList(),
)
