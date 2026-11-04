package com.coffeepeek.admin.utils.image

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class PhotoCacheKey(val url: String, val sessionScope: Any)
internal data class PhotoTransfer(val status: Int, val headers: Headers, val bytes: ByteArray = byteArrayOf())
internal data class AuthorizedPhoto(
    val bytes: ByteArray,
    val permission: PhotoPermission,
    val reusablePermission: PhotoPermission? = null,
)

/** Bounded encoded bytes only. Expired bytes never imply permission to display them. */
internal class ProtectedPhotoCache(
    private val scope: CoroutineScope,
    private val clock: PhotoCacheClock = SystemPhotoCacheClock,
    private val maxEntries: Int = 48,
    private val maxBytes: Int = 12 * 1024 * 1024,
) {
    private data class Stored(val bytes: ByteArray, val etag: String, val permission: PhotoPermission)
    private class Pending(val result: CompletableDeferred<AuthorizedPhoto>, val job: Job, var users: Int = 0)
    private val mutex = Mutex()
    private val stored = linkedMapOf<PhotoCacheKey, Stored>()
    private val pending = mutableMapOf<PhotoCacheKey, Pending>()
    private var byteCount = 0

    suspend fun read(
        key: PhotoCacheKey,
        jwtDeadline: Long?,
        current: () -> Boolean,
        revalidate: Boolean = false,
        transfer: suspend (String?) -> PhotoTransfer,
    ): AuthorizedPhoto {
        check(current()) { "Фото недоступно" }
        val work = mutex.withLock {
            stored[key]?.takeIf { !revalidate && it.permission.remaining(clock) > 0 && (jwtDeadline == null || jwtDeadline > clock.wallMillis()) }?.let {
                check(current()) { "Фото недоступно" }
                stored.remove(key); stored[key] = it
                return AuthorizedPhoto(it.bytes, it.permission, it.permission)
            }
            pending.getOrPut(key) {
                val result = CompletableDeferred<AuthorizedPhoto>()
                val job = scope.launch(start = CoroutineStart.LAZY) {
                    try { result.complete(load(key, jwtDeadline, current, transfer, result)) }
                    catch (failure: Throwable) {
                        withContext(NonCancellable) { mutex.withLock {
                            if (pending[key]?.result === result) evict(key)
                        } }
                        result.completeExceptionally(failure)
                        if (failure is CancellationException) throw failure
                    } finally { withContext(NonCancellable) { mutex.withLock {
                        if (pending[key]?.result === result) pending.remove(key)
                    } } }
                }
                Pending(result, job)
            }.also { it.users++ }
        }
        work.job.start()
        try {
            val result = work.result.await()
            check(current() && result.permission.remaining(clock) > 0) { "Фото недоступно" }
            return result
        } finally { withContext(NonCancellable) { mutex.withLock {
            if (--work.users == 0) {
                if (pending[key] === work) pending.remove(key)
                work.job.cancel()
            }
        } } }
    }

    private suspend fun load(key: PhotoCacheKey, jwtDeadline: Long?, current: () -> Boolean, transfer: suspend (String?) -> PhotoTransfer, owner: CompletableDeferred<AuthorizedPhoto>): AuthorizedPhoto {
        val previous = mutex.withLock { stored[key] }
        check(current() && (jwtDeadline == null || jwtDeadline > clock.wallMillis())) { "Фото недоступно" }
        val start = clock.monotonicMillis()
        val response = transfer(previous?.etag)
        currentCoroutineContext().ensureActive()
        val received = clock.monotonicMillis()
        val wall = clock.wallMillis()
        check(current() && (jwtDeadline == null || jwtDeadline > wall)) { "Фото недоступно" }
        requirePhotoAccess(response.status)
        val maxFresh = if (isShelfBagPhoto(key.url)) 300_000L else 20_000L
        val policy = PhotoCachePolicy.read(response.headers, start, received, wall, maxFresh)
        check(policy.allowsOnlineDisplay) { "Фото недоступно" }
        val responseTag = response.headers.getAll(HttpHeaders.ETag)?.singleOrNull()?.takeIf(::validPhotoEtag)
        val bytes = when (response.status) {
            200 -> response.bytes.also { check(it.isNotEmpty()) { "Фото недоступно" } }
            304 -> {
                check(previous != null && responseTag == previous.etag && policy.stores) { "Фото недоступно" }
                // This is the exact retained representation bound to the conditional request.
                previous.bytes
            }
            else -> error("Фото недоступно")
        }
        // A 304 has its own current authorization headers. Never infer a new lease
        // from old metadata, and never carry an old max-age forward when absent.
        val fresh = minOf(policy.freshMillis, jwtDeadline?.let { (it - wall).coerceAtLeast(0) } ?: policy.freshMillis)
        val freshness = PhotoPermission(received + fresh, wall + fresh)
        // A successful online access check also authorizes this individual presentation.
        // Missing/zero freshness cannot produce a later fresh-cache hit.
        val displayMillis = if (policy.maxAgeMillis > 0 && policy.hasValidDate) minOf(
            policy.onlineMillis, jwtDeadline?.let { (it - wall).coerceAtLeast(0) } ?: 20_000,
        ) else minOf(
            (20_000 - (received - start).coerceAtLeast(0)).coerceAtLeast(0),
            jwtDeadline?.let { (it - wall).coerceAtLeast(0) } ?: 20_000,
        )
        check(displayMillis > 0) { "Фото недоступно" }
        mutex.withLock {
            currentCoroutineContext().ensureActive()
            check(current()) { "Фото недоступно" }
            check(pending[key]?.result === owner) { "Фото недоступно" }
            if (response.status == 304) check(stored[key] === previous) { "Фото недоступно" }
            evict(key)
            if (policy.stores && responseTag != null && bytes.size <= maxBytes && maxEntries > 0) {
                stored[key] = Stored(bytes, responseTag, freshness)
                byteCount += bytes.size
                while (stored.size > maxEntries || byteCount > maxBytes) evict(stored.keys.first())
            }
        }
        return AuthorizedPhoto(
            bytes,
            PhotoPermission(received + displayMillis, wall + displayMillis),
            freshness.takeIf { policy.stores && responseTag != null && fresh > 0 },
        )
    }

    suspend fun discardInvalidScopes(valid: (Any) -> Boolean) = mutex.withLock {
        stored.keys.filterNot { valid(it.sessionScope) }.forEach(::evict)
        // Snapshot keys: removing an entry invalidates retained Map.Entry references on Native.
        pending.keys.filterNot { valid(it.sessionScope) }.forEach { key ->
            pending.remove(key)?.let { work ->
                work.result.cancel()
                work.job.cancel()
            }
        }
    }

    private fun evict(key: PhotoCacheKey) { stored.remove(key)?.let { byteCount -= it.bytes.size } }
}

private fun validPhotoEtag(value: String): Boolean = Regex("(?:W/)?\"[^\"\\r\\n]+\"").matches(value)

internal val protectedPhotoCache = ProtectedPhotoCache(CoroutineScope(SupervisorJob() + Dispatchers.Default))
