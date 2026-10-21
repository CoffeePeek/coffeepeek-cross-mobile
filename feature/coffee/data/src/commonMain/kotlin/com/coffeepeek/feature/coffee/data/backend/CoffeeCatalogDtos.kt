package com.coffeepeek.feature.coffee.data.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Wire representations for the existing catalogue endpoints; never exposed by the factory.
@Serializable
internal data class CoffeeSearchRequestDto(val q: String, val filters: JsonObject, val page: Int, val pageSize: Int = 20)

@Serializable
internal data class CoffeeResponseDto<T>(val isSuccess: Boolean, val data: T? = null, val message: String? = null) {
    fun requireData(): T {
        check(isSuccess) { message ?: "Coffee request failed" }
        return checkNotNull(data) { "Coffee response has no data" }
    }
}

@Serializable
internal data class CoffeePageDto(val items: List<CoffeeDto>, val currentPage: Int, val totalPages: Int, val totalCount: Int? = null)

@Serializable
internal data class CoffeeDto(
    val address: CoffeeAddressDto,
    val name: String,
    val roaster: CoffeeRoasterDto,
    val coverPhoto: CoffeePhotoDto? = null,
    val countries: List<CoffeeCountryDto> = emptyList(),
    val classification: CoffeeClassificationDto? = null,
    val matchingOffers: List<CoffeeOfferDto> = emptyList(),
    val description: String? = null,
    val photos: List<CoffeePhotoDto> = emptyList(),
    val offers: List<CoffeeOfferDto> = emptyList(),
    val productKind: String? = null,
    val productForm: String? = null,
    val tasteDescriptors: List<String> = emptyList(),
    val catalogCheckedAtUtc: String? = null,
)

@Serializable
internal data class CoffeeAddressDto(val slug: String, val canonicalPath: String? = null)

@Serializable
internal data class CoffeeRoasterDto(val name: String, val coverPhoto: CoffeePhotoDto? = null, val address: CoffeeAddressDto? = null)

@Serializable
internal data class CoffeePhotoDto(val fullUrl: String? = null, val urls: CoffeePhotoUrlsDto? = null)

@Serializable
internal data class CoffeePhotoUrlsDto(val card: String? = null, val thumbnail: String? = null, val detail: String? = null)

@Serializable
internal data class CoffeeCountryDto(val code: String, val nameRu: String? = null, val nameEn: String? = null)

@Serializable
internal data class CoffeeClassificationDto(
    val tasteGroups: List<String> = emptyList(),
    val roastLevel: String? = null,
    val acidity: String? = null,
)

@Serializable
internal data class CoffeeOfferDto(
    val price: Double? = null,
    val currency: String? = null,
    val weightGrams: Int? = null,
    val offerKey: String? = null,
    val availability: String? = null,
    val sellerName: String? = null,
    val sourceUrl: String? = null,
    val grind: String? = null,
    val brewPurpose: String? = null,
    val availabilityScope: String? = null,
    val checkedAtUtc: String? = null,
)

@Serializable
internal data class CoffeeFilterGroupDto(val code: String, val name: String, val values: List<CoffeeFilterValueDto>)

@Serializable
internal data class CoffeeFilterValueDto(val code: String, val name: String, val sortOrder: Int = 0)
