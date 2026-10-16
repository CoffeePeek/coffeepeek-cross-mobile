package com.coffeepeek.admin.analytics

import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException

internal fun safeAnalyticsError(error: Throwable): SafeAnalyticsError {
    if (error is com.coffeepeek.domain.model.CheckInOperationFailure) error.cause?.let { return safeAnalyticsError(it) }
    val status = ((error as? ResponseException)?.response?.status?.value ?: (error as? com.coffeepeek.api.utils.ApiException)?.statusCode)?.takeIf { it in 100..599 }
    val category = when {
        error is CancellationException -> "cancelled"
        status == 401 -> "authentication"
        status == 403 -> "permission"
        status == 404 -> "not_found"
        status == 429 -> "rate_limited"
        status != null && status >= 500 -> "server"
        status != null && status >= 400 -> "validation"
        error is IOException -> "network"
        else -> "unknown"
    }
    return SafeAnalyticsError(category, status)
}

internal fun safeStatusError(status: Int): SafeAnalyticsError? = when {
    status < 400 -> null
    status == 401 -> SafeAnalyticsError("authentication", status)
    status == 403 -> SafeAnalyticsError("permission", status)
    status == 404 -> SafeAnalyticsError("not_found", status)
    status == 429 -> SafeAnalyticsError("rate_limited", status)
    status >= 500 -> SafeAnalyticsError("server", status)
    else -> SafeAnalyticsError("validation", status)
}
