package com.coffeepeek.feature.contributions.data.mapper

import com.coffeepeek.feature.contributions.data.backend.ChangeRequestDto
import com.coffeepeek.feature.contributions.data.backend.ContactsDto
import com.coffeepeek.feature.contributions.data.backend.GalleryDto
import com.coffeepeek.feature.contributions.data.backend.MenuDto
import com.coffeepeek.feature.contributions.data.backend.MenuItemDto
import com.coffeepeek.feature.contributions.data.backend.PageDto
import com.coffeepeek.feature.contributions.data.backend.PayloadDto
import com.coffeepeek.feature.contributions.data.backend.RoasterSubmissionDto
import com.coffeepeek.feature.contributions.data.backend.RoasterSubmittedDto
import com.coffeepeek.feature.contributions.data.backend.ShopSubmissionDto
import com.coffeepeek.feature.contributions.data.backend.UploadedPhotoDto
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.ChangeMenu
import com.coffeepeek.feature.contributions.domain.model.ChangePayload
import com.coffeepeek.feature.contributions.domain.model.ChangePhotos
import com.coffeepeek.feature.contributions.domain.model.ChangeRequest
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.Contacts
import com.coffeepeek.feature.contributions.domain.model.MenuEntry
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.domain.model.Paged
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmission
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmitted
import com.coffeepeek.feature.contributions.domain.model.ShopSubmission
import com.coffeepeek.feature.contributions.domain.model.UploadedPhoto

internal fun <T, R> PageDto<T>.toDomain(transform: (T) -> R) = Paged(items.map(transform), totalItems, totalPages, currentPage)

internal fun String.toStatus(): ModerationStatus = ModerationStatus.entries.firstOrNull { it.name == this } ?: error("Unknown moderation status: $this")
internal fun String.toSection(): ChangeSection = ChangeSection.entries.firstOrNull { it.name == this } ?: error("Unknown change section: $this")
private fun String.toAvailability(): Availability = Availability.entries.firstOrNull { it.name == this } ?: Availability.Unknown

internal fun ShopSubmissionDto.toDomain() = ShopSubmission(id, name, address, moderationStatus.toStatus(), rejectedReason, publishedShop?.slug?.takeIf(String::isNotBlank))

internal fun RoasterSubmissionDto.toDomain() = RoasterSubmission(id, name, about, moderationStatus.toStatus(), rejectedReason)

internal fun RoasterSubmittedDto.toDomain(message: String?) = RoasterSubmitted(roasterId, status, isAddressValidated, message.orEmpty())

internal fun ChangeRequestDto.toDomain() = ChangeRequest(
    id = id,
    shopSlug = shop?.slug.orEmpty(),
    section = section.toSection(),
    payload = payload.toDomain(),
    status = status.toStatus(),
    rejectionReason = rejectionReason,
    createdAtUtc = createdAtUtc,
    updatedAtUtc = updatedAtUtc,
)

internal fun PayloadDto.toDomain() = ChangePayload(
    description = description,
    contacts = contacts?.let { Contacts(it.phoneNumber, it.email, it.siteLink, it.instagramLink) },
    photos = photos?.let { ChangePhotos(it.retainedPhotoIds, it.newPhotos.map(UploadedPhotoDto::toDomain)) },
    tagIds = tags,
    roasterIds = roasters,
    equipmentIds = equipments,
    brewMethodIds = brewMethods,
    menu = menu?.let { menu ->
        ChangeMenu(menu.items.map { MenuEntry(it.slug, it.availability.toAvailability(), it.price) }, menu.retainedPhotoIds, menu.newPhotos.map(UploadedPhotoDto::toDomain))
    },
)

internal fun UploadedPhotoDto.toDomain() = UploadedPhoto(fileName, contentType, storageKey, size)
internal fun UploadedPhoto.toDto() = UploadedPhotoDto(fileName, contentType, storageKey, size)

/** Blank values are not sent. */
internal fun Contacts.toDto() = ContactsDto(phone.clean(), email.clean(), website.clean(), instagram.clean())

internal fun Contacts.isEmpty() = phone.clean() == null && email.clean() == null && website.clean() == null && instagram.clean() == null

internal fun galleryDto(retained: List<String>, uploaded: List<UploadedPhoto>) = GalleryDto(retained, uploaded.map(UploadedPhoto::toDto))

internal fun menuDto(items: List<MenuEntry>, retained: List<String>, uploaded: List<UploadedPhoto>) =
    MenuDto(items.map { MenuItemDto(it.slug, it.availability.name, it.price) }, retained, uploaded.map(UploadedPhoto::toDto))

private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)
