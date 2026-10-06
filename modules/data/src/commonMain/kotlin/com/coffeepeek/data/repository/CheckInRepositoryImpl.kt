package com.coffeepeek.data.repository

import com.coffeepeek.domain.model.validateConsumedDrink

import com.coffeepeek.api.model.request.CreateCheckInReq
import com.coffeepeek.api.model.response.CheckInDto
import com.coffeepeek.api.model.response.shop.variantOr
import com.coffeepeek.api.model.response.shop.RatingDto
import com.coffeepeek.api.service.CheckInApiService
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CreateCheckInInput
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.ReviewRating

class CheckInRepositoryImpl(
    private val checkInApiService: CheckInApiService,
    private val photoRepository: PhotoRepository,
    private val fileUrlResolver: FileUrlResolver,
) : CheckInRepository {

    override suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit> = runCatching {
        validateConsumedDrink(input.drinkSlug, input.customDrinkName)?.let { error(it) }
        val placeRating = input.placeRating
        val serviceRating = input.serviceRating
        val coffeeRating = input.coffeeRating
        val rating = if (placeRating != null && serviceRating != null && coffeeRating != null) {
            RatingDto(
                place = placeRating,
                service = serviceRating,
                coffee = coffeeRating,
            )
        } else {
            null
        }

        val uploadedPhotos = photoRepository.uploadReviewPhotos(input.photos)
            .getOrThrow()
            .toUploadedPhotoReqs()
            .takeIf { it.isNotEmpty() }

        checkInApiService.createCheckIn(
            CreateCheckInReq(
                coffeeShopId = input.shopId,
                isPublic = input.isPublic,
                visitedAt = input.visitedAtIso,
                drinkSlug = input.drinkSlug,
                customDrinkName = input.customDrinkName?.trim(),
                header = input.header?.takeIf { it.isNotBlank() },
                note = input.note?.takeIf { it.isNotBlank() },
                photos = uploadedPhotos,
                rating = rating,
            )
        ).getOrThrow()
    }

    override suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<PagedResult<CheckIn>> =
        checkInApiService.getMyCheckIns(page, pageSize).map { response ->
            PagedResult(
                items = response.checkIns.map { dto ->
                    dto.toDomain()
                },
                totalCount = response.totalItems,
                totalPages = response.totalPages,
                currentPage = response.currentPage,
            )
        }

    override suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>> =
        checkInApiService.getMyCheckIns(from, to, pageSize).map { response ->
            response.checkIns.map { it.toDomain() }
        }

    private fun CheckInDto.toDomain() = CheckIn(
        drinkSlug = drinkSlug,
        customDrinkName = customDrinkName,
        drinkNameRu = drinkNameRu,
        drinkNameEn = drinkNameEn,
        id = id,
        shopId = shop?.slug.orEmpty(),
        shopName = shopName.orEmpty(),
        note = note.orEmpty(),
        createdAt = createdAt,
        reviewId = reviewId,
        visitedAt = visitedAt,
        photoUrls = photos.mapNotNull { photo ->
            fileUrlResolver.resolve(photo.storageKey, photo.urls.variantOr(photo.fullUrl) { it.fullscreen })
        },
        photoThumbnailUrls = photos.mapNotNull { photo ->
            fileUrlResolver.resolve(photo.storageKey, photo.urls.variantOr(photo.fullUrl) { it.thumbnail })
        },
        rating = rating?.let {
            ReviewRating(place = it.place, service = it.service, coffee = it.coffee)
        },
    )
}
