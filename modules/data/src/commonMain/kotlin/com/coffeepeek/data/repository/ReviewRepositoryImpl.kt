package com.coffeepeek.data.repository

import com.coffeepeek.domain.model.validateConsumedDrink

import com.coffeepeek.api.model.request.SendReviewReq
import com.coffeepeek.api.model.request.UpdateReviewReq
import com.coffeepeek.api.model.response.shop.RatingDto
import com.coffeepeek.api.service.ReviewApiService
import com.coffeepeek.data.mapper.ShopMapper.toDomain
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.data.mapper.toDto
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CreateReviewInput
import com.coffeepeek.domain.model.HelpfulVote
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewRating
import com.coffeepeek.domain.model.ReviewSubmission
import com.coffeepeek.domain.model.UpdateReviewInput
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.ReviewRepository

class ReviewRepositoryImpl(
    private val reviewApiService: ReviewApiService,
    private val photoRepository: PhotoRepository,
    private val fileUrlResolver: FileUrlResolver,
) : ReviewRepository {
    override suspend fun submitReviewReport(reviewId: String, text: String): Result<String> =
        reviewApiService.submitReviewReport(reviewId, text)


    override suspend fun createReview(input: CreateReviewInput): Result<Unit> = runCatching {
        validateConsumedDrink(input.drinkSlug, input.customDrinkName)?.let { error(it) }
        val photos = photoRepository.uploadShopPhotos(input.photos).getOrThrow()
        reviewApiService.createReview(
            SendReviewReq(
                shopId = input.shopId,
                drinkSlug = input.drinkSlug,
                customDrinkName = input.customDrinkName?.trim(),
                header = input.header,
                comment = input.comment,
                rating = RatingDto(
                    place = input.placeRating,
                    service = input.serviceRating,
                    coffee = input.coffeeRating,
                ),
                photos = photos.toUploadedPhotoReqs().takeIf { it.isNotEmpty() },
            ),
        ).getOrThrow()
    }

    // ponytail: UpdateCoffeeShopReviewCommand has no photos field, so input.photos is ignored on
    // edit. Restore photo upload here + a photos field on the command if the backend adds support.
    override suspend fun updateReview(reviewId: String, input: UpdateReviewInput): Result<Unit> = runCatching {
        require(!input.clearDrink || (input.drinkSlug == null && input.customDrinkName == null)) { "Удаление напитка нельзя совмещать с выбором" }
        validateConsumedDrink(input.drinkSlug, input.customDrinkName)?.let { error(it) }
        reviewApiService.updateReview(
            reviewId = reviewId,
            req = UpdateReviewReq(
                clearDrink = input.clearDrink,
                reviewId = reviewId,
                drinkSlug = input.drinkSlug,
                customDrinkName = input.customDrinkName?.trim(),
                header = input.header,
                comment = input.comment,
                rating = RatingDto(
                    place = input.placeRating,
                    service = input.serviceRating,
                    coffee = input.coffeeRating,
                ),
            )
        ).getOrThrow()
    }

    override suspend fun setReviewHelpful(reviewId: String, helpful: Boolean): Result<HelpfulVote> =
        reviewApiService.setReviewHelpful(reviewId, helpful).map {
            HelpfulVote(isHelpful = it.isHelpful, helpfulCount = it.helpfulCount)
        }

    override suspend fun getUserReviews(
        userId: String,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<Review>> =
        reviewApiService.getUserReviews(userId, page, pageSize).map { response ->
            PagedResult(
                items = response.reviewDtos.map { it.toDomain(fileUrlResolver) },
                totalCount = response.totalItems,
                totalPages = response.totalPages,
                currentPage = response.currentPage,
            )
        }

    override suspend fun getMyReviewSubmissions(
        status: ModerationStatus,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<ReviewSubmission>> =
        reviewApiService.getMyModerationReviews(status.toDto(), page, pageSize).map { response ->
            PagedResult(
                items = response.reviewDtos.map { dto ->
                    ReviewSubmission(
                        review = Review(
                            drinkSlug = dto.drinkSlug,
                            customDrinkName = dto.customDrinkName,
                            drinkNameRu = dto.drinkNameRu,
                            drinkNameEn = dto.drinkNameEn,
                            id = dto.id,
                            moderationReviewId = dto.id,
                            shopId = dto.shop?.slug.orEmpty(),
                            userId = "",
                            username = dto.userName.orEmpty(),
                            header = dto.header.orEmpty(),
                            comment = dto.comment,
                            rating = ReviewRating(dto.rating.place, dto.rating.service, dto.rating.coffee),
                            createdAt = dto.createdAt,
                            photoUrls = dto.photos.mapNotNull { fileUrlResolver.resolve(it.storageKey, it.fullUrl) },
                        ),
                        status = dto.moderationStatus.toDomain(),
                        rejectedReason = dto.rejectedReason,
                    )
                },
                totalCount = response.totalItems,
                totalPages = response.totalPages,
                currentPage = response.currentPage,
            )
        }
}
