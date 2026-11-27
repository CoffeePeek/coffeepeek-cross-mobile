package com.coffeepeek.core.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException

private val authorizationGuard = AttributeKey<() -> Boolean>("coffeepeek.authorizationGuard")

/** Optional per-request account/permission lease. Checked again after a suspending credential refresh. */
fun HttpRequestBuilder.authorizeWhile(allowed: () -> Boolean) {
    attributes.put(authorizationGuard, allowed)
}

internal fun HttpRequestBuilder.requireCurrentAuthorization() {
    if (attributes.getOrNull(authorizationGuard)?.invoke() == false) throw CancellationException("Request authorization changed")
}
