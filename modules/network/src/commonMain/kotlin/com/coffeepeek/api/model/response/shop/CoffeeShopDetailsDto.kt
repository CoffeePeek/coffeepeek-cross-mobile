package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.response.CheckInDto
import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.serialization.FlexibleIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CoffeeShopDetailsDto(
    @SerialName("address") val address: PublicAddressDto,
    @SerialName("city") val city: PublicAddressDto? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("photos") val photos: List<ShortPhotoDto> = emptyList(),
    @SerialName("rating") val rating: Double = 0.0,
    @Serializable(with = FlexibleIntSerializer::class) val checkInCount: Int = 0,
    val checkIns: List<CheckInDto> = emptyList(),
    @SerialName("reviews") val reviews: List<ReviewDto> = emptyList(),
    @SerialName("userCheckIns") val userCheckIns: List<CheckInDto> = emptyList(),
    @SerialName("isFavorite") val isFavorite: Boolean = false,
    @SerialName("isVisited") val isVisited: Boolean = false,
    @SerialName("canCreateReview") val canCreateReview: Boolean? = null,
    @SerialName("existingReviewId") val existingReviewId: String? = null,
    @SerialName("isOpen") val isOpen: Boolean = false,
    @SerialName("isNew") val isNew: Boolean = false,
    @SerialName("priceRange") val priceRange: JsonElement? = null,
    @SerialName("type") val type: JsonElement? = null,
    @SerialName("coffeeFocus") val coffeeFocus: JsonElement? = null,
    @SerialName("location") val location: LocationDto? = null,
    @SerialName("beans") val coffeeBeans: List<CatalogItemDto> = emptyList(),
    @SerialName("roasters") val roasters: List<CatalogItemDto> = emptyList(),
    @SerialName("equipments") val equipments: List<CatalogItemDto> = emptyList(),
    @SerialName("brewMethods") val brewMethods: List<CatalogItemDto> = emptyList(),
    @SerialName("tags") val tags: JsonElement? = null,
    @SerialName("shopTags") val shopTags: JsonElement? = null,
    @SerialName("shopContact") val shopContact: ShopContactDto? = null,
    @SerialName("schedules") val schedules: List<ScheduleDto>? = null,
    @SerialName("menu") val menu: ShopMenuDto? = null,
)

@Serializable
data class ShopContactDto(
    @SerialName("instagramLink") val instagramLink: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("siteLink") val siteLink: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
)
