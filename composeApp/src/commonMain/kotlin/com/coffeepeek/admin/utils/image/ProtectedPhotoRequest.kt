package com.coffeepeek.admin.utils.image

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.Url
import kotlinx.coroutines.sync.Semaphore

internal val protectedPhotoRequests = Semaphore(4)

/**
 * A photo request that hangs on a dead or stalled connection is cut by the client's short timeouts, and the same request is made once more at once
 * on a fresh connection. The permit of the failed attempt is released between the attempts, so a hung request never holds a slot of the queue.
 * Anything but a timeout (access denied, a bad status, cancellation) is not retried here.
 */
internal suspend fun <T> retryOnPhotoTimeout(attempt: suspend () -> T): T {
    try {
        return attempt()
    } catch (_: ConnectTimeoutException) {
    } catch (_: SocketTimeoutException) {
    } catch (_: HttpRequestTimeoutException) {
    }
    return attempt()
}

internal class PhotoAccessDenied : IllegalStateException("Фото недоступно")

internal fun requirePhotoAccess(status: Int) {
    if (status in setOf(401, 403, 404, 410)) throw PhotoAccessDenied()
}

/** Owner-scoped photos: check-in photos and shelf bag cards, loaded with the session and never from a shared cache. */
internal fun isProtectedCheckInPhoto(url: String): Boolean =
    Regex("/api/v1/(check-ins|shelf/bags)/[^/?]+/photos/[^/?]+").containsMatchIn(url)

internal fun isImmutablePhotoVariant(url: String): Boolean = runCatching {
    val path = if (url.startsWith('/')) url.substringBefore('?').substringBefore('#') else Url(url).encodedPath
    Regex("/api/v1/check-ins/[^/]+/photos/[^/]+/variants/[A-Za-z0-9_-]+/[1-9][0-9]*/(thumb|feed|detail)\\.(jpg|jpeg|png|webp)").matches(path)
}.getOrDefault(false)

internal fun originalCheckInPhoto(url: String): String? =
    url.takeIf { isProtectedCheckInPhoto(it) && "/variants/" in it }?.substringBefore("/variants/")
