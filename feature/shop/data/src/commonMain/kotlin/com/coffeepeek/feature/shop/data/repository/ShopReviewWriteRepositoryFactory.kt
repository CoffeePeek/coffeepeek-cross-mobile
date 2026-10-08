package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.data.backend.ShopPhotoUpload
import com.coffeepeek.feature.shop.data.backend.ShopPhotoUploadBackend
import com.coffeepeek.feature.shop.data.backend.ShopPhotoPurpose
import com.coffeepeek.feature.shop.data.backend.ShopReviewWriteBackend
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_REVIEW_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopReviewUpdateInput
import com.coffeepeek.feature.shop.domain.repository.ShopReviewWriteRepository
import com.coffeepeek.feature.shop.domain.usecase.validateShopReviewText
import io.ktor.client.HttpClient

/** Composition supplies the authenticated API client and a separate unauthenticated upload client. */
fun createShopReviewWriteRepository(
    apiClient: HttpClient,
    uploadClient: HttpClient,
): ShopReviewWriteRepository = DefaultShopReviewWriteRepository(
    ShopPhotoUploadBackend(apiClient, uploadClient), ShopReviewWriteBackend(apiClient),
)

private class DefaultShopReviewWriteRepository(
    private val photos: ShopPhotoUploadBackend,
    private val writes: ShopReviewWriteBackend,
) : ShopReviewWriteRepository {
    override suspend fun create(input: ShopReviewCreateInput): Result<Unit> = requestResult {
        require(input.shopId.isNotBlank()) { "Invalid shop ID" }
        validate(input.header, input.comment, input.rating)
        require(input.photos.size <= MAX_SHOP_REVIEW_PHOTOS) { "Too many review photos" }
        val uploaded = photos.upload(
            input.photos.map { ShopPhotoUpload(it.bytes, it.fileName, it.contentType) },
            purpose = ShopPhotoPurpose.Review,
        )
            .getOrThrow()
        writes.create(input.shopId, input.header.trim(), input.comment.trim(), input.rating,
            uploaded).getOrThrow()
    }

    override suspend fun update(input: ShopReviewUpdateInput): Result<Unit> = requestResult {
        validate(input.header, input.comment, input.rating)
        // Current server command has no photos field; the domain edit input cannot carry them.
        writes.update(input.reviewId, input.header.trim(), input.comment.trim(), input.rating)
            .getOrThrow()
    }
}

private fun validate(header: String, comment: String, rating: ShopRating) {
    require(validateShopReviewText(header, comment).isValid) { "Invalid review text" }
    require(rating.place in 1..5 && rating.service in 1..5 && rating.coffee in 1..5) {
        "Invalid review rating"
    }
}
