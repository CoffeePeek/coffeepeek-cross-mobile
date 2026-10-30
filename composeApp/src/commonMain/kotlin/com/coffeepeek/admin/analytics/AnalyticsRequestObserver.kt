package com.coffeepeek.admin.analytics

import com.coffeepeek.core.network.RequestObservation
import com.coffeepeek.core.network.RequestObserver
import com.coffeepeek.core.session.SessionReader

internal fun analyticsRequestObserver(service: AnalyticsService, sessions: SessionReader) =
    RequestObserver { category, method ->
        val operation = service.operationScope()
        val account = sessions.peekSession()?.userId
        RequestObservation { status, error, duration, retried ->
            if (sessions.peekSession()?.userId == account) {
                val failure = status?.let(::safeStatusError) ?: error?.let(::safeAnalyticsError)
                if (failure?.category != "cancelled") service.track(
                    AnalyticsEvent.API_REQUEST_COMPLETED,
                    requestProperties(category, method, status, error, duration, retried) +
                        failure?.let { mapOf("error_category" to it.category) }.orEmpty(),
                    operation,
                )
            }
        }
    }

private fun requestProperties(
    category: String,
    method: String,
    status: Int?,
    error: Throwable?,
    duration: Long,
    retried: Boolean,
): Map<String, Any> = mapOf(
    "category" to category,
    "method" to method,
    "outcome" to if (error == null && (status ?: 0) in 100..399) "success" else "failure",
    "duration_ms" to duration,
    "auth_retried" to retried,
) + status?.takeIf { it in 100..599 }?.let { mapOf("status_code" to it) }.orEmpty()
