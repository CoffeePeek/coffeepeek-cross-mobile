package com.coffeepeek.feature.shelf.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shelf.domain.model.ShelfFailure
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/** Every failure becomes a [ShelfFailure]; cancellation is rethrown by [requestResult]. */
internal suspend fun <T> shelfRequestResult(block: suspend () -> T): Result<T> {
    val result = requestResult(block)
    val failure = result.exceptionOrNull() ?: return result
    return Result.failure(failure.toShelfFailure())
}

internal fun Throwable.toShelfFailure(): ShelfFailure = when (this) {
    is ShelfFailure -> this
    is ResponseException -> shelfServerFailure(response.status.value)
    is HttpRequestTimeoutException -> ShelfFailure(ShelfIssue.Timeout)
    is IOException -> ShelfFailure(ShelfIssue.Offline)
    is SerializationException -> ShelfFailure(ShelfIssue.InvalidResponse)
    // The gateway validator can reject 5xx before the feature reads the body.
    else -> ShelfFailure(if (message == "Сервис временно недоступен. Попробуйте чуть позже.") ShelfIssue.Unavailable else ShelfIssue.RequestFailed)
}

/** The shelf contract distinguishes outcomes by status alone (`docs/api/shelf-client.md`). */
internal fun shelfServerFailure(status: Int): ShelfFailure = ShelfFailure(when {
    status == 401 || status == 403 -> ShelfIssue.Unauthorized
    status == 404 -> ShelfIssue.NotFound
    status == 409 -> ShelfIssue.Stale
    status == 429 -> ShelfIssue.RateLimited
    status >= 500 -> ShelfIssue.Unavailable
    status == 400 || status == 422 -> ShelfIssue.InvalidInput
    else -> ShelfIssue.RequestFailed
}, statusCode = status)
