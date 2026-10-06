package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.PublicAddressDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogItemDto(
    @SerialName("name") val name: String? = null,
    @SerialName("slug") val slug: String = "",
    @SerialName("address") val address: PublicAddressDto? = null,
    @SerialName("coverPhoto") val coverPhoto: ShortPhotoDto? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
) {
    val key: String get() = address?.slug ?: slug
}
