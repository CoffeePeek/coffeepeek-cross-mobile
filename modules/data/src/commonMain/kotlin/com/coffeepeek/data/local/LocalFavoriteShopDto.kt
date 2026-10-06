package com.coffeepeek.data.local

import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.ShopLocation
import kotlinx.serialization.Serializable

@Serializable
data class LocalFavoriteShopDto(
    val id: String,
    val publicAddress: PublicAddressDto? = null,
    val title: String,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val cityName: String? = null,
    val priceRange: String? = null,
    val photoUrl: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isOpen: Boolean = false,
    val tags: List<String> = emptyList(),
    val brewMethods: List<String> = emptyList(),
    // Kept for compatibility with favorites saved before multiple roaster logos were supported.
    val roasterPhotoUrl: String? = null,
    val roasterPhotoUrls: List<String> = emptyList(),
) {
    fun toDomain(): CoffeeShopDetails = CoffeeShopDetails(
        shop = CoffeeShop(
            id = id,
            publicAddress = publicAddress?.toDomain(),
            title = title,
            rating = rating,
            reviewCount = reviewCount,
            cityName = cityName,
            priceRange = priceRange,
            photoUrl = photoUrl,
            address = address,
            isOpen = isOpen,
            isFavorite = true,
            tags = tags,
            brewMethods = brewMethods,
            roasterPhotoUrls = roasterPhotoUrls.ifEmpty { listOfNotNull(roasterPhotoUrl) },
            location = savedLocation(),
        ),
        location = savedLocation(),
    )

    companion object {
        fun from(shop: CoffeeShop, address: String? = null) = LocalFavoriteShopDto(
            id = shop.id,
            publicAddress = shop.publicAddress?.let {
                PublicAddressDto(it.slug, it.canonicalPath, it.revision, it.isAlias)
            },
            title = shop.title,
            rating = shop.rating,
            reviewCount = shop.reviewCount,
            cityName = shop.cityName,
            priceRange = shop.priceRange,
            photoUrl = shop.photoUrl,
            address = address ?: shop.address,
            latitude = shop.location?.latitude,
            longitude = shop.location?.longitude,
            isOpen = shop.isOpen,
            tags = shop.tags,
            brewMethods = shop.brewMethods,
            roasterPhotoUrl = shop.roasterPhotoUrls.firstOrNull(),
            roasterPhotoUrls = shop.roasterPhotoUrls,
        )
    }

    private fun savedLocation(): ShopLocation? =
        if (address != null || latitude != null || longitude != null) {
            ShopLocation(address = address, latitude = latitude, longitude = longitude)
        } else {
            null
        }
}
