package com.coffeepeek.feature.contributions.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Only the fields the forms read; everything else the server sends is ignored. */
@Serializable
internal data class RefCityDto(@SerialName("address") val address: RefSlugDto, @SerialName("name") val name: String)

@Serializable
internal data class RefSlugDto(@SerialName("slug") val slug: String? = null)

@Serializable
internal data class RefCatalogItemDto(
    @SerialName("name") val name: String? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("address") val address: RefSlugDto? = null,
) {
    /** The public address slug when there is one, else the plain slug. */
    val key: String get() = address?.slug?.takeIf(String::isNotBlank) ?: slug.orEmpty()
}

@Serializable
internal data class RefDrinksDto(@SerialName("drinks") val drinks: List<RefDrinkDto> = emptyList())

@Serializable
internal data class RefDrinkDto(
    @SerialName("slug") val slug: String,
    @SerialName("nameRu") val nameRu: String = "",
    @SerialName("nameEn") val nameEn: String = "",
    @SerialName("sortOrder") val sortOrder: Int = 0,
)

/** imgproxy variants; without the proxy the backend fills all four with the full url. */
@Serializable
internal data class RefPhotoUrlsDto(
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("fullscreen") val fullscreen: String? = null,
)

@Serializable
internal data class RefPhotoDto(
    @SerialName("id") val id: String = "",
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("urls") val urls: RefPhotoUrlsDto? = null,
    @SerialName("sortIndex") val sortIndex: JsonElement? = null,
)

@Serializable
internal data class RefContactDto(
    @SerialName("instagramLink") val instagramLink: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("siteLink") val siteLink: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
)

@Serializable
internal data class RefMenuItemDto(
    @SerialName("slug") val slug: String,
    @SerialName("availability") val availability: String = "Unknown",
    @SerialName("price") val price: JsonElement? = null,
)

@Serializable
internal data class RefMenuDto(
    @SerialName("items") val items: List<RefMenuItemDto> = emptyList(),
    @SerialName("photos") val photos: List<RefPhotoDto> = emptyList(),
)

@Serializable
internal data class RefShopDto(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("photos") val photos: List<RefPhotoDto> = emptyList(),
    @SerialName("roasters") val roasters: List<RefCatalogItemDto> = emptyList(),
    @SerialName("equipments") val equipments: List<RefCatalogItemDto> = emptyList(),
    @SerialName("brewMethods") val brewMethods: List<RefCatalogItemDto> = emptyList(),
    @SerialName("tags") val tags: JsonElement? = null,
    @SerialName("shopTags") val shopTags: JsonElement? = null,
    @SerialName("shopContact") val shopContact: RefContactDto? = null,
    @SerialName("menu") val menu: RefMenuDto? = null,
)
