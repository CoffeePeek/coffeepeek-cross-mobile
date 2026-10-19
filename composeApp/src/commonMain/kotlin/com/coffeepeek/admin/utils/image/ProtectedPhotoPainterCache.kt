package com.coffeepeek.admin.utils.image

import androidx.compose.ui.graphics.painter.Painter
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.ceil

internal data class AuthorizedPhotoPainter(val painter: Painter, val permission: PhotoPermission)

/** Decoded variants may be reused only within the server's original cache permission. */
@OptIn(ExperimentalAtomicApi::class)
internal class ProtectedPhotoPainterCache(
    private val clock: PhotoCacheClock = SystemPhotoCacheClock,
    private val current: (Any) -> Boolean = ProtectedPhotoSession::current,
    private val maxBytes: Long = 24L * 1024 * 1024,
    private val maxEntries: Int = 48,
) {
    init {
        require(maxBytes >= 0)
        require(maxEntries >= 0)
    }

    private data class Entry(val key: PhotoCacheKey, val value: AuthorizedPhotoPainter, val bytes: Long)
    private data class State(val entries: List<Entry> = emptyList(), val bytes: Long = 0)
    private val state = AtomicReference(State())

    fun get(url: String, sessionScope: Any): AuthorizedPhotoPainter? {
        val key = PhotoCacheKey(url, sessionScope)
        while (true) {
            val before = state.load()
            val retained = before.entries.filter(::valid)
            val entry = retained.firstOrNull { it.key == key }
            val promoted = if (entry == null) retained else retained.filterNot { it === entry } + entry
            if (state.compareAndSet(before, State(promoted, promoted.sumOf { it.bytes }))) {
                return entry?.value?.takeIf { current(sessionScope) && it.permission.remaining(clock) > 0 }
            }
        }
    }

    /** Pass reusablePermission, never an online-only presentation permission. */
    fun store(url: String, sessionScope: Any, painter: Painter, reusablePermission: PhotoPermission?) {
        val key = PhotoCacheKey(url, sessionScope)
        val bytes = painter.estimatedBytes()
        val candidate = reusablePermission?.let { Entry(key, AuthorizedPhotoPainter(painter, it), bytes ?: Long.MAX_VALUE) }
            ?.takeIf { bytes != null && bytes <= maxBytes && maxEntries > 0 && isImmutablePhotoVariant(url) && valid(it) }
        while (true) {
            val before = state.load()
            val retained = before.entries.filter { it.key != key && valid(it) }.toMutableList()
            var retainedBytes = retained.sumOf { it.bytes }
            if (candidate != null && valid(candidate)) {
                while (retained.isNotEmpty() && (retained.size >= maxEntries || retainedBytes > maxBytes - candidate.bytes)) {
                    retainedBytes -= retained.removeAt(0).bytes
                }
                retained.add(candidate)
                retainedBytes += candidate.bytes
            }
            if (state.compareAndSet(before, State(retained.toList(), retainedBytes))) return
        }
    }

    fun discardInvalidScopes(validScope: (Any) -> Boolean) {
        while (true) {
            val before = state.load()
            val retained = before.entries.filter { validScope(it.key.sessionScope) && valid(it) }
            if (state.compareAndSet(before, State(retained, retained.sumOf { it.bytes }))) return
        }
    }

    private fun valid(entry: Entry): Boolean = current(entry.key.sessionScope) && entry.value.permission.remaining(clock) > 0
}

// Unknown dimensions cannot safely participate in a retained-pixel budget.
private fun Painter.estimatedBytes(): Long? {
    val width = intrinsicSize.width
    val height = intrinsicSize.height
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return null
    val bytes = ceil(width.toDouble()) * ceil(height.toDouble()) * 4
    return bytes.takeIf { it < Long.MAX_VALUE.toDouble() }?.toLong()
}

internal val protectedPhotoPainterCache = ProtectedPhotoPainterCache()
