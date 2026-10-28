package com.coffeepeek.feature.contributions.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.contributions.data.backend.ContributionsBackend
import com.coffeepeek.feature.contributions.data.backend.CreateChangeRequest
import com.coffeepeek.feature.contributions.data.backend.CreateRoasterRequest
import com.coffeepeek.feature.contributions.data.backend.CreateShopRequest
import com.coffeepeek.feature.contributions.data.backend.IntervalRequest
import com.coffeepeek.feature.contributions.data.backend.PayloadDto
import com.coffeepeek.feature.contributions.data.backend.PhotoUploadBackend
import com.coffeepeek.feature.contributions.data.backend.ScheduleRequest
import com.coffeepeek.feature.contributions.data.backend.UpdateChangeRequest
import com.coffeepeek.feature.contributions.data.backend.UploadPurpose
import com.coffeepeek.feature.contributions.data.mapper.galleryDto
import com.coffeepeek.feature.contributions.data.mapper.isEmpty
import com.coffeepeek.feature.contributions.data.mapper.menuDto
import com.coffeepeek.feature.contributions.data.mapper.toDomain
import com.coffeepeek.feature.contributions.data.mapper.toDto
import com.coffeepeek.feature.contributions.domain.model.ChangeDraft
import com.coffeepeek.feature.contributions.domain.model.ChangeRequest
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.domain.model.NewRoaster
import com.coffeepeek.feature.contributions.domain.model.NewShop
import com.coffeepeek.feature.contributions.domain.model.Paged
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmission
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmitted
import com.coffeepeek.feature.contributions.domain.model.ShopSubmission
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.domain.rules.issue
import com.coffeepeek.feature.contributions.domain.rules.isSendable
import com.coffeepeek.feature.contributions.domain.rules.shiftWeek
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

/**
 * [client] is the authenticated API client, [uploadClient] sends bytes to presigned URLs without credentials; the caller revokes access on account
 * replacement. [utcOffsetMinutes] is the device's current UTC offset: weekly hours have no date, so the shop's hours are moved to UTC with it.
 */
fun createContributionsRepository(client: HttpClient, uploadClient: HttpClient, utcOffsetMinutes: () -> Int): ContributionsRepository =
    RemoteContributionsRepository(ContributionsBackend(client), PhotoUploadBackend(client, uploadClient), utcOffsetMinutes)

private class RemoteContributionsRepository(
    private val backend: ContributionsBackend,
    private val uploads: PhotoUploadBackend,
    private val utcOffsetMinutes: () -> Int,
) : ContributionsRepository {
    override suspend fun shops(status: ModerationStatus, page: Int, pageSize: Int): Result<Paged<ShopSubmission>> = requestResult {
        backend.shops(status.name, page.coerceAtLeast(1), pageSize.coerceIn(1, 100)).getOrThrow().toDomain { it.toDomain() }
    }

    override suspend fun roasters(status: ModerationStatus, page: Int, pageSize: Int): Result<Paged<RoasterSubmission>> = requestResult {
        backend.roasters(status.name, page.coerceAtLeast(1), pageSize.coerceIn(1, 100)).getOrThrow().toDomain { it.toDomain() }
    }

    override suspend fun changes(status: ModerationStatus?, page: Int, pageSize: Int, shopSlug: String?, section: ChangeSection?): Result<Paged<ChangeRequest>> =
        requestResult {
            backend.changes(status?.name, page.coerceAtLeast(1), pageSize.coerceIn(1, 100), shopSlug, section?.name).getOrThrow().toDomain { it.toDomain() }
        }

    override suspend fun change(id: String): Result<ChangeRequest> = requestResult { backend.change(id).getOrThrow().toDomain() }

    override suspend fun submitShop(shop: NewShop): Result<Unit> = requestResult {
        if (!shop.isSendable()) throw ContributionFailure(FailureKind.Invalid)
        val (shopPhotos, menuPhotos) = coroutineScope {
            val photos = async { uploads.upload(shop.photos, UploadPurpose.Shop).getOrThrow() }
            val menu = async { uploads.upload(shop.menuPhotos, UploadPurpose.Menu).getOrThrow() }
            photos.await() to menu.await()
        }
        val week = shiftWeek(shop.week, -utcOffsetMinutes())
        backend.createShop(
            CreateShopRequest(
                name = shop.name.trim(),
                address = shop.address.trim(),
                city = shop.cityId,
                latitude = shop.latitude,
                longitude = shop.longitude,
                description = shop.description?.trim()?.takeIf(String::isNotEmpty),
                priceRange = shop.priceRange?.name,
                shopContact = shop.contacts.takeUnless { it.isEmpty() }?.toDto(),
                schedules = week.takeIf { it.isNotEmpty() }?.map { day ->
                    ScheduleRequest(dayNames[day.day], day.closed, day.intervals.takeIf { it.isNotEmpty() }?.map { IntervalRequest(it.open, it.close) })
                },
                shopPhotos = shopPhotos.takeIf { it.isNotEmpty() }?.map { it.toDto() },
                menuPhotos = menuPhotos.takeIf { it.isNotEmpty() }?.map { it.toDto() },
                equipments = shop.equipmentIds.takeIf { it.isNotEmpty() },
                beans = shop.beanIds.takeIf { it.isNotEmpty() },
                roasters = shop.roasterIds.takeIf { it.isNotEmpty() },
                brewMethods = shop.brewMethodIds.takeIf { it.isNotEmpty() },
            ),
        ).getOrThrow()
    }

    override suspend fun submitRoaster(roaster: NewRoaster): Result<RoasterSubmitted> = requestResult {
        if (!roaster.isSendable()) throw ContributionFailure(FailureKind.Invalid)
        val photos = uploads.upload(roaster.photos, UploadPurpose.Roaster).getOrThrow()
        val (submitted, message) = backend.submitRoaster(
            CreateRoasterRequest(
                name = roaster.name.trim(),
                about = roaster.about.clean(),
                city = roaster.cityId,
                address = roaster.address.clean(),
                instagramLink = roaster.instagramLink.clean(),
                siteLink = roaster.siteLink.clean(),
                photos = photos.takeIf { it.isNotEmpty() }?.map { it.toDto() },
            ),
        ).getOrThrow()
        submitted.toDomain(message)
    }

    override suspend fun submitChange(draft: ChangeDraft): Result<ChangeRequest> = requestResult {
        val payload = payloadOf(draft)
        backend.createChange(CreateChangeRequest(draft.shopSlug, draft.section.name, payload)).getOrThrow().toDomain()
    }

    override suspend fun editChange(id: String, draft: ChangeDraft): Result<ChangeRequest> = requestResult {
        val payload = payloadOf(draft)
        backend.updateChange(id, UpdateChangeRequest(draft.section.name, payload)).getOrThrow().toDomain()
    }

    /** Refuses a draft that breaks a limit, uploads its new photos, and returns the payload of its own section only. */
    private suspend fun payloadOf(draft: ChangeDraft): PayloadDto {
        if (draft.issue() != null) throw ContributionFailure(FailureKind.Invalid)
        return when (draft) {
            // A blank description is sent as no text, as the legacy form did.
            is ChangeDraft.Description -> PayloadDto(description = draft.text.trim().takeIf(String::isNotEmpty))
            is ChangeDraft.ContactsChange -> PayloadDto(contacts = draft.contacts.toDto())
            is ChangeDraft.Photos -> PayloadDto(
                photos = galleryDto(draft.retainedIds, draft.uploaded + uploads.upload(draft.newPhotos, UploadPurpose.Shop).getOrThrow()),
            )
            is ChangeDraft.Selection -> when (draft.section) {
                ChangeSection.Tags -> PayloadDto(tags = draft.ids)
                ChangeSection.Roasters -> PayloadDto(roasters = draft.ids)
                ChangeSection.Equipment -> PayloadDto(equipments = draft.ids)
                else -> PayloadDto(brewMethods = draft.ids)
            }
            is ChangeDraft.Menu -> PayloadDto(
                menu = menuDto(
                    draft.items,
                    draft.retainedPhotoIds,
                    draft.uploaded + uploads.upload(draft.newPhotos, UploadPurpose.Menu).getOrThrow(),
                    draft.expectedInputRevision,
                ),
            )
        }
    }
}

private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)
