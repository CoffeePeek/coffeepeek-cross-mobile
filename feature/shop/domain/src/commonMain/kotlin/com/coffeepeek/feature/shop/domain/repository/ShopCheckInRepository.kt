package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPublication

interface ShopCheckInRepository {
    suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>>
    /** Returns the server's check-in ID so publication can be checked without guessing from visibility. */
    suspend fun create(input: ShopCheckInCreateInput): Result<String>
}

interface ShopCheckInPublicationReader {
    suspend fun status(checkInId: String): Result<ShopCheckInPublication>

    data object None : ShopCheckInPublicationReader {
        override suspend fun status(checkInId: String) = Result.success(ShopCheckInPublication.Unknown)
    }
}

/** The request may have reached the server; check history before submitting again. */
class ShopCheckInCreationUnconfirmed(cause: Throwable? = null) :
    Exception("Check-in creation could not be confirmed", cause)

/** A photo failed before the check-in creation request was started. */
class ShopCheckInPhotoUploadFailure(cause: Throwable) : Exception("Photo upload failed", cause)

/** The server refused the check-in. [status] is the HTTP status; [serverMessage] is the server's own text when it sent one. */
class ShopCheckInRejection(val status: Int, val serverMessage: String?) :
    Exception(serverMessage ?: "Check-in request was rejected ($status)")
