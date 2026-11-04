package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.DataResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CoffeeDrinkDefinitionDto(
    @SerialName("slug") val slug: String,
    @SerialName("nameRu") val nameRu: String = "",
    @SerialName("nameEn") val nameEn: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("sortOrder") val sortOrder: Int = 0,
)

@Serializable
data class GetDrinksResponseDto(
    @SerialName("drinks") val drinks: List<CoffeeDrinkDefinitionDto> = emptyList(),
) : DataResponse()

@Serializable
data class ShopMenuItemDto(
    @SerialName("slug") val slug: String,
    @SerialName("nameRu") val nameRu: String = "",
    @SerialName("nameEn") val nameEn: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("availability") val availability: String = "Unknown",
    @SerialName("price") val price: JsonElement? = null,
    @SerialName("currency") val currency: String = "BYN",
    @SerialName("volumeMl") val volumeMl: JsonElement? = null,
    @SerialName("source") val source: String = "Parsed",
)

@Serializable
data class ShortPhotoMetadataDto(
    @SerialName("id") val id: String = "",
    @SerialName("fileName") val fileName: String = "",
    @SerialName("storageKey") val storageKey: String = "",
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("urls") val urls: PhotoUrlsDto? = null,
    @SerialName("sortIndex") val sortIndex: Int = 0,
)

@Serializable
data class ShopMenuDto(
    @SerialName("capturedAtUtc") val capturedAtUtc: String? = null,
    @SerialName("updatedAtUtc") val updatedAtUtc: String? = null,
    @SerialName("currency") val currency: String = "BYN",
    @SerialName("parseStatus") val parseStatus: String? = null,
    @SerialName("parseError") val parseError: String? = null,
    @SerialName("suggestedPriceRange") val suggestedPriceRange: String? = null,
    @SerialName("items") val items: List<ShopMenuItemDto> = emptyList(),
    @SerialName("photos") val photos: List<ShortPhotoMetadataDto> = emptyList(),
    @SerialName("inputRevision") val inputRevision: Long? = null,
)
