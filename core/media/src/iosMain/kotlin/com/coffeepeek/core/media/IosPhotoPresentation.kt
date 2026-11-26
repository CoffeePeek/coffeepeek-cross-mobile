@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.core.media

import com.coffeepeek.core.media.image.PhotoDisplayPermission
import com.coffeepeek.core.media.image.PhotoPermission
import com.coffeepeek.core.media.image.ProtectedPhotoSession
import com.coffeepeek.core.media.image.SystemPhotoCacheClock
import com.coffeepeek.core.media.image.isProtectedCheckInPhoto
import com.coffeepeek.core.media.image.isImmutablePhotoVariant
import io.kamel.core.config.ResourceConfigBuilder
import io.kamel.core.getOrNull
import io.ktor.utils.io.toByteArray
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

suspend fun ImageUrlFetcher.loadIosImage(source: ImageUrl): UIImage? = withContext(Dispatchers.Default) {
    try {
        val config = ResourceConfigBuilder(currentCoroutineContext()).build()
        fetch(source, config).mapNotNull { it.getOrNull()?.toByteArray()?.toImage() }.firstOrNull()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

/** One-shot cache warming never renews a display grant or requests a protected original. */
suspend fun ImageUrlFetcher.prefetchIosImage(source: ImageUrl, canLoad: () -> Boolean) {
    if (!canLoad()) return
    if (!isProtectedCheckInPhoto(source.url)) {
        loadIosImage(source)
        return
    }
    if (!isImmutablePhotoVariant(source.url)) return
    val scope = ProtectedPhotoSession.changes.value
    if (canLoad() && ProtectedPhotoSession.current(scope)) {
        loadIosImage(source.copy(permissionScope = scope, displayPermission = PhotoDisplayPermission(revalidate = false)))
    }
}

/** Emits cleared frames when an authorization expires or the photo access scope changes. */
@OptIn(ExperimentalCoroutinesApi::class)
fun ImageUrlFetcher.observeIosViewerImage(source: ImageUrl, canLoad: () -> Boolean): Flow<UIImage?> = flow {
    if (isProtectedCheckInPhoto(source.url)) {
        var started = false
        emitAll(ProtectedPhotoSession.changes.transformLatest { scope ->
            if (started) emit(null)
            started = true
            emitAll(authorizedFrames(source, scope, canLoad))
        })
    } else {
        val image = if (canLoad()) loadIosImage(source) else null
        emit(image.takeIf { canLoad() })
    }
}

private fun ImageUrlFetcher.authorizedFrames(source: ImageUrl, scope: Any, canLoad: () -> Boolean) = flow {
    var renew = false
    while (canLoad() && ProtectedPhotoSession.current(scope)) {
        val permission = PhotoDisplayPermission(revalidate = renew)
        val image = loadIosImage(source.copy(permissionScope = scope, displayPermission = permission))
        val grant = permission.permission.value
        if (image == null || grant == null || !canLoad() || !ProtectedPhotoSession.current(scope) ||
            grant.remaining(SystemPhotoCacheClock) == 0L
        ) {
            emit(null)
            return@flow
        }
        emit(image)
        waitForExpiry(grant) { canLoad() && ProtectedPhotoSession.current(scope) }
        emit(null)
        renew = true
    }
}

private suspend fun waitForExpiry(grant: PhotoPermission, current: () -> Boolean) {
    while (current()) {
        val remaining = grant.remaining(SystemPhotoCacheClock)
        if (remaining <= 0L) return
        delay(remaining.coerceAtMost(1000L))
    }
}

private fun ByteArray.toImage(): UIImage? {
    if (isEmpty()) return null
    val data = usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    return UIImage.imageWithData(data)
}
