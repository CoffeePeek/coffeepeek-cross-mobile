package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption

interface ShopCheckInRepository {
    suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>>
    suspend fun create(input: ShopCheckInCreateInput): Result<Unit>
}

/** The request may have reached the server; check history before submitting again. */
class ShopCheckInCreationUnconfirmed(cause: Throwable? = null) :
    Exception("Check-in creation could not be confirmed", cause)

/** A photo failed before the check-in creation request was started. */
class ShopCheckInPhotoUploadFailure(cause: Throwable) : Exception("Photo upload failed", cause)
