package com.coffeepeek.feature.contributions.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PageDto<T>(
    @SerialName("items") val items: List<T> = emptyList(),
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
)

@Serializable internal data class AddressDto(@SerialName("slug") val slug: String = "")

@Serializable
internal data class ShopSubmissionDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("address") val address: String? = null,
    @SerialName("moderationStatus") val moderationStatus: String,
    @SerialName("rejectedReason") val rejectedReason: String? = null,
    @SerialName("publishedShop") val publishedShop: AddressDto? = null,
)

@Serializable
internal data class RoasterSubmissionDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("about") val about: String? = null,
    @SerialName("moderationStatus") val moderationStatus: String,
    @SerialName("rejectedReason") val rejectedReason: String? = null,
)

@Serializable
internal data class RoasterSubmittedDto(
    @SerialName("roasterId") val roasterId: String,
    @SerialName("status") val status: String,
    @SerialName("isAddressValidated") val isAddressValidated: Boolean = false,
)

@Serializable
internal data class UploadedPhotoDto(
    @SerialName("fileName") val fileName: String,
    @SerialName("contentType") val contentType: String,
    @SerialName("storageKey") val storageKey: String,
    @SerialName("size") val size: Long,
)

@Serializable
internal data class ContactsDto(
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("siteLink") val siteLink: String? = null,
    @SerialName("instagramLink") val instagramLink: String? = null,
)

@Serializable
internal data class GalleryDto(
    @SerialName("retainedPhotoIds") val retainedPhotoIds: List<String> = emptyList(),
    @SerialName("newPhotos") val newPhotos: List<UploadedPhotoDto> = emptyList(),
)

@Serializable
internal data class MenuItemDto(
    @SerialName("slug") val slug: String,
    @SerialName("availability") val availability: String = "Unknown",
    @SerialName("price") val price: Double? = null,
    @SerialName("volumeMl") val volumeMl: Int? = null,
)

@Serializable
internal data class MenuDto(
    @SerialName("items") val items: List<MenuItemDto> = emptyList(),
    @SerialName("retainedPhotoIds") val retainedPhotoIds: List<String> = emptyList(),
    @SerialName("newPhotos") val newPhotos: List<UploadedPhotoDto> = emptyList(),
    @SerialName("expectedInputRevision") val expectedInputRevision: Long? = null,
)

@Serializable
internal data class PayloadDto(
    @SerialName("description") val description: String? = null,
    @SerialName("contacts") val contacts: ContactsDto? = null,
    @SerialName("photos") val photos: GalleryDto? = null,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("roasters") val roasters: List<String>? = null,
    @SerialName("equipments") val equipments: List<String>? = null,
    @SerialName("menu") val menu: MenuDto? = null,
    @SerialName("brewMethods") val brewMethods: List<String>? = null,
)

@Serializable
internal data class ChangeRequestDto(
    @SerialName("id") val id: String,
    @SerialName("shop") val shop: AddressDto? = null,
    @SerialName("section") val section: String,
    @SerialName("payload") val payload: PayloadDto = PayloadDto(),
    @SerialName("status") val status: String,
    @SerialName("rejectionReason") val rejectionReason: String? = null,
    @SerialName("createdAtUtc") val createdAtUtc: String,
    @SerialName("updatedAtUtc") val updatedAtUtc: String? = null,
)

@Serializable
internal data class CreateChangeRequest(
    @SerialName("shop") val shop: String,
    @SerialName("section") val section: String,
    @SerialName("payload") val payload: PayloadDto,
)

@Serializable
internal data class UpdateChangeRequest(
    @SerialName("section") val section: String,
    @SerialName("payload") val payload: PayloadDto,
)

@Serializable
internal data class IntervalRequest(
    @SerialName("openTime") val openTime: String,
    @SerialName("closeTime") val closeTime: String,
)

@Serializable
internal data class ScheduleRequest(
    @SerialName("dayOfWeek") val dayOfWeek: String,
    @SerialName("isClosed") val isClosed: Boolean,
    @SerialName("intervals") val intervals: List<IntervalRequest>? = null,
)

@Serializable
internal data class CreateShopRequest(
    @SerialName("name") val name: String,
    @SerialName("address") val address: String,
    @SerialName("city") val city: String,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("priceRange") val priceRange: String? = null,
    @SerialName("shopContact") val shopContact: ContactsDto? = null,
    @SerialName("schedules") val schedules: List<ScheduleRequest>? = null,
    @SerialName("shopPhotos") val shopPhotos: List<UploadedPhotoDto>? = null,
    @SerialName("menuPhotos") val menuPhotos: List<UploadedPhotoDto>? = null,
    @SerialName("equipments") val equipments: List<String>? = null,
    @SerialName("beans") val beans: List<String>? = null,
    @SerialName("roasters") val roasters: List<String>? = null,
    @SerialName("brewMethods") val brewMethods: List<String>? = null,
)

@Serializable
internal data class CreateRoasterRequest(
    @SerialName("name") val name: String,
    @SerialName("about") val about: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("instagramLink") val instagramLink: String? = null,
    @SerialName("siteLink") val siteLink: String? = null,
    @SerialName("photos") val photos: List<UploadedPhotoDto>? = null,
)
