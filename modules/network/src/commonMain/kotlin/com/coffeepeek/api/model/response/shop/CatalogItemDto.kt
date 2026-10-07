package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.serialization.FlexibleIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogItemDto(
    @SerialName("name") val name: String? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("address") val address: CatalogAddressDto? = null,
    @SerialName("coverPhoto") val coverPhoto: ShortPhotoDto? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("sortOrder")
    @Serializable(with = FlexibleIntSerializer::class)
    val sortOrder: Int = 0,
    @SerialName("tags") val tags: List<CatalogItemDto> = emptyList(),
    @SerialName("coffeeShopsCount")
    @Serializable(with = FlexibleIntSerializer::class)
    val coffeeShopsCount: Int = 0,
    @SerialName("coffeeProductsCount")
    @Serializable(with = FlexibleIntSerializer::class)
    val coffeeProductsCount: Int = 0,
    @SerialName("availableCoffeeProducts")
    @Serializable(with = FlexibleIntSerializer::class)
    val availableCoffeeProducts: Int = 0,
) {
    val key: String get() = address?.slug?.takeIf(String::isNotBlank) ?: slug.orEmpty()
}

@Serializable
data class CatalogAddressDto(
    @SerialName("slug") val slug: String? = null,
    @SerialName("canonicalPath") val canonicalPath: String? = null,
    @SerialName("revision")
    @Serializable(with = FlexibleIntSerializer::class)
    val revision: Int = 0,
    @SerialName("isAlias") val isAlias: Boolean = false,
)
