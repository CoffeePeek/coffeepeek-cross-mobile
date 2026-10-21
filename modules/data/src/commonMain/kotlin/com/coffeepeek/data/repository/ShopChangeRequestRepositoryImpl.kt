package com.coffeepeek.data.repository

import com.coffeepeek.api.utils.requestResult
import com.coffeepeek.api.model.request.CreateShopChangeRequestBody
import com.coffeepeek.api.model.request.UpdateShopChangeRequestBody
import com.coffeepeek.api.service.ShopChangeRequestApiService
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.data.mapper.toDto
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.ShopChangeDraft
import com.coffeepeek.domain.model.ShopChangeGallery
import com.coffeepeek.domain.model.ShopChangeMenu
import com.coffeepeek.domain.model.ShopChangePayload
import com.coffeepeek.domain.model.ShopChangeRequest
import com.coffeepeek.domain.model.ShopChangeSection
import com.coffeepeek.domain.model.UploadedPhotoMeta
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.ShopChangeRequestRepository

class ShopChangeRequestRepositoryImpl(
    private val api: ShopChangeRequestApiService,
    private val photoRepository: PhotoRepository,
) : ShopChangeRequestRepository {

    override suspend fun create(draft: ShopChangeDraft): Result<ShopChangeRequest> = requestResult {
        val payload = draft.toPayload()
        api.create(
            CreateShopChangeRequestBody(
                shopId = draft.shopId,
                section = draft.section.toDto(),
                payload = payload.toDto(),
            ),
        ).getOrThrow().toDomain()
    }

    override suspend fun getMine(
        page: Int,
        pageSize: Int,
        status: ModerationStatus?,
        shopId: String?,
        section: ShopChangeSection?,
    ): Result<PagedResult<ShopChangeRequest>> =
        api.getMine(page, pageSize, status?.toDto(), shopId, section?.toDto()).map { it.toDomain() }

    override suspend fun getById(id: String): Result<ShopChangeRequest> =
        api.getById(id).map { it.toDomain() }

    override suspend fun update(id: String, draft: ShopChangeDraft): Result<ShopChangeRequest> = requestResult {
        val payload = draft.toPayload()
        api.update(
            id,
            UpdateShopChangeRequestBody(
                section = draft.section.toDto(),
                payload = payload.toDto(),
            ),
        ).getOrThrow().toDomain()
    }

    private suspend fun ShopChangeDraft.toPayload(): ShopChangePayload = when (section) {
        ShopChangeSection.Description -> ShopChangePayload(description = description)
        ShopChangeSection.Contacts -> ShopChangePayload(contacts = contacts)
        ShopChangeSection.Tags -> ShopChangePayload(tagIds = tagIds.orEmpty())
        ShopChangeSection.Roasters -> ShopChangePayload(roasterIds = roasterIds.orEmpty())
        ShopChangeSection.Equipment -> ShopChangePayload(equipmentIds = equipmentIds.orEmpty())
        ShopChangeSection.BrewMethods -> ShopChangePayload(brewMethodIds = brewMethodIds.orEmpty())
        ShopChangeSection.Photos -> ShopChangePayload(
            photos = ShopChangeGallery(
                retainedPhotoIds = retainedPhotoIds,
                newPhotos = alreadyUploadedPhotos + uploadShop(newPhotos),
            ),
        )
        ShopChangeSection.Menu -> ShopChangePayload(
            menu = ShopChangeMenu(
                items = menuItems.orEmpty(),
                retainedPhotoIds = retainedMenuPhotoIds,
                newPhotos = alreadyUploadedMenuPhotos + uploadMenu(newMenuPhotos),
            ),
        )
    }

    private suspend fun uploadShop(photos: List<PendingPhotoUpload>): List<UploadedPhotoMeta> {
        if (photos.isEmpty()) return emptyList()
        return photoRepository.uploadShopPhotos(photos).getOrThrow()
    }

    private suspend fun uploadMenu(photos: List<PendingPhotoUpload>): List<UploadedPhotoMeta> {
        if (photos.isEmpty()) return emptyList()
        return photoRepository.uploadMenuPhotos(photos.take(4)).getOrThrow()
    }
}
