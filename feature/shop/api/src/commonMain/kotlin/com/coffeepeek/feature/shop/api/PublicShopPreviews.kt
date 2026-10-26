package com.coffeepeek.feature.shop.api

/** Public map/card enrichment, excluding personal check-ins, menu and favorite state. */
fun interface PublicShopPreviews {
    suspend fun load(slug: String): Result<PublicShopPreview>
}

data class PublicShopPreview(
    val coverPhotoUrl: String?,
    val detailPhotoUrl: String?,
    val rating: Double?,
    val reviewCount: Int,
    val isOpen: Boolean,
    val hours: PublicShopOpeningHours?,
    val coordinates: PublicShopCoordinates?,
)

/** First available local interval, preserving the map's existing summary policy. */
data class PublicShopOpeningHours(val isClosed: Boolean, val openTime: String? = null, val closeTime: String? = null)

data class PublicShopCoordinates(val latitude: Double, val longitude: Double)
