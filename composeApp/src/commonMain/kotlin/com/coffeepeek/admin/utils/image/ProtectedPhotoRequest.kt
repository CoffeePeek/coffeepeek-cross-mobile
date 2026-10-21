package com.coffeepeek.admin.utils.image

import kotlinx.coroutines.sync.Semaphore
import io.ktor.http.Url

internal val protectedPhotoRequests = Semaphore(4)

internal class PhotoAccessDenied : IllegalStateException("Фото недоступно")

internal fun requirePhotoAccess(status: Int) {
    if (status in setOf(401, 403, 404, 410)) throw PhotoAccessDenied()
}

internal fun isProtectedCheckInPhoto(url: String): Boolean =
    Regex("/api/v1/check-ins/[^/?]+/photos/[^/?]+").containsMatchIn(url)

internal fun isImmutablePhotoVariant(url: String): Boolean = runCatching {
    val path = if (url.startsWith('/')) url.substringBefore('?').substringBefore('#') else Url(url).encodedPath
    Regex("/api/v1/check-ins/[^/]+/photos/[^/]+/variants/[A-Za-z0-9_-]+/[1-9][0-9]*/(thumb|feed|detail)\\.(jpg|jpeg|png|webp)").matches(path)
}.getOrDefault(false)

internal fun originalCheckInPhoto(url: String): String? =
    url.takeIf { isProtectedCheckInPhoto(it) && "/variants/" in it }?.substringBefore("/variants/")
