package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.data.backend.ShopCheckInBackend
import com.coffeepeek.feature.shop.data.backend.ShopPhotoUpload
import com.coffeepeek.feature.shop.data.backend.ShopPhotoUploadBackend
import com.coffeepeek.feature.shop.data.backend.ShopPhotoPurpose
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.usecase.validateShopCheckIn
import io.ktor.client.HttpClient

/** Composition retains ownership of the authenticated API and public-upload clients. */
fun createShopCheckInRepository(
    apiClient: HttpClient,
    uploadClient: HttpClient,
): ShopCheckInRepository = DefaultShopCheckInRepository(
    ShopCheckInBackend(apiClient), ShopPhotoUploadBackend(apiClient, uploadClient),
)

private class DefaultShopCheckInRepository(
    private val backend: ShopCheckInBackend,
    private val photos: ShopPhotoUploadBackend,
) : ShopCheckInRepository {
    override suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>> = backend.getDrinkOptions()

    override suspend fun create(input: ShopCheckInCreateInput): Result<Unit> = requestResult {
        val error = validateShopCheckIn(input)
        require(error == null) { "Invalid check-in input: $error" }
        val uploaded = photos.upload(input.photos.map {
            ShopPhotoUpload(it.bytes, it.fileName, it.contentType)
        }, purpose = ShopPhotoPurpose.CheckIn).getOrThrow()
        backend.create(
            shopSlug = input.shopSlug,
            visitedAtIso = input.visitedAtIso,
            visibility = input.visibility.name,
            drinkSlug = input.drinkSlug,
            customDrinkName = input.customDrinkName?.trim(),
            text = input.text.trim(),
            rating = input.rating,
            photos = uploaded,
        ).getOrThrow()
    }
}
