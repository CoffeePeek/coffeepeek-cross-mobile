package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.request.MenuItemAvailabilityDto
import com.coffeepeek.api.model.request.ModerationStatusDto
import com.coffeepeek.api.model.request.ShopChangeContactsDto
import com.coffeepeek.api.model.request.ShopChangeGalleryDto
import com.coffeepeek.api.model.request.ShopChangeMenuDto
import com.coffeepeek.api.model.request.ShopChangePayloadDto
import com.coffeepeek.api.model.request.ShopChangeRequestDto
import com.coffeepeek.api.model.request.ShopChangeRequestPageDto
import com.coffeepeek.api.model.request.ShopChangeSectionDto
import com.coffeepeek.api.model.request.UpdateShopMenuItemRequestDto
import com.coffeepeek.api.model.request.UploadedPhotoReq
import com.coffeepeek.data.repository.toUploadedPhotoReqs
import com.coffeepeek.domain.model.MenuItemAvailability
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.ShopChangeContacts
import com.coffeepeek.domain.model.ShopChangeGallery
import com.coffeepeek.domain.model.ShopChangeMenu
import com.coffeepeek.domain.model.ShopChangeMenuItem
import com.coffeepeek.domain.model.ShopChangePayload
import com.coffeepeek.domain.model.ShopChangeRequest
import com.coffeepeek.domain.model.ShopChangeSection
import com.coffeepeek.domain.model.UploadedPhotoMeta

internal fun ShopChangeSection.toDto() = ShopChangeSectionDto.valueOf(name)
internal fun ModerationStatus.toDto() = ModerationStatusDto.valueOf(name)
internal fun MenuItemAvailability.toDto() = MenuItemAvailabilityDto.valueOf(name)

internal fun ShopChangeSectionDto.toDomain() = ShopChangeSection.valueOf(name)
internal fun ModerationStatusDto.toDomain() = ModerationStatus.valueOf(name)
internal fun MenuItemAvailabilityDto.toDomain() = MenuItemAvailability.valueOf(name)

internal fun ShopChangeRequestDto.toDomain() = ShopChangeRequest(
    id = id,
    shopId = shop?.slug.orEmpty(),
    section = section.toDomain(),
    payload = payload.toDomain(),
    status = status.toDomain(),
    reviewedAtUtc = reviewedAtUtc,
    rejectionReason = rejectionReason,
    createdAtUtc = createdAtUtc,
    updatedAtUtc = updatedAtUtc,
)

internal fun ShopChangeRequestPageDto.toDomain() = PagedResult(
    items = items.map { it.toDomain() },
    totalCount = totalItems,
    totalPages = totalPages,
    currentPage = currentPage,
)

internal fun ShopChangePayloadDto.toDomain() = ShopChangePayload(
    description = description,
    contacts = contacts?.toDomain(),
    photos = photos?.toDomain(),
    tagIds = tagIds,
    roasterIds = roasterIds,
    equipmentIds = equipmentIds,
    menu = menu?.toDomain(),
    brewMethodIds = brewMethodIds,
)

internal fun ShopChangeContactsDto.toDomain() = ShopChangeContacts(
    phoneNumber = phoneNumber,
    email = email,
    siteLink = siteLink,
    instagramLink = instagramLink,
)

internal fun ShopChangeGalleryDto.toDomain() = ShopChangeGallery(
    retainedPhotoIds = retainedPhotoIds,
    newPhotos = newPhotos.map { it.toMeta() },
)

internal fun ShopChangeMenuDto.toDomain() = ShopChangeMenu(
    items = items.map { it.toDomain() },
    retainedPhotoIds = retainedPhotoIds,
    newPhotos = newPhotos.map { it.toMeta() },
)

internal fun UpdateShopMenuItemRequestDto.toDomain() = ShopChangeMenuItem(
    slug = slug,
    availability = availability.toDomain(),
    price = price,
    volumeMl = volumeMl,
)

internal fun UploadedPhotoReq.toMeta() = UploadedPhotoMeta(
    fileName = fileName,
    contentType = contentType,
    storageKey = storageKey,
    size = size,
)

internal fun ShopChangePayload.toDto() = ShopChangePayloadDto(
    description = description,
    contacts = contacts?.toDto(),
    photos = photos?.toDto(),
    tagIds = tagIds,
    roasterIds = roasterIds,
    equipmentIds = equipmentIds,
    menu = menu?.toDto(),
    brewMethodIds = brewMethodIds,
)

internal fun ShopChangeContacts.toDto() = ShopChangeContactsDto(
    phoneNumber = phoneNumber,
    email = email,
    siteLink = siteLink,
    instagramLink = instagramLink,
)

internal fun ShopChangeGallery.toDto() = ShopChangeGalleryDto(
    retainedPhotoIds = retainedPhotoIds,
    newPhotos = newPhotos.toUploadedPhotoReqs(),
)

internal fun ShopChangeMenu.toDto() = ShopChangeMenuDto(
    items = items.map { it.toDto() },
    retainedPhotoIds = retainedPhotoIds,
    newPhotos = newPhotos.toUploadedPhotoReqs(),
)

internal fun ShopChangeMenuItem.toDto() = UpdateShopMenuItemRequestDto(
    slug = slug,
    availability = availability.toDto(),
    price = price,
    volumeMl = volumeMl,
)
