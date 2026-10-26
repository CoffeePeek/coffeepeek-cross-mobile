package com.coffeepeek.core.media.image

import androidx.compose.ui.graphics.ImageBitmap
import io.kamel.core.cache.Cache
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** Bounds retained decoded pixels as well as entry count. Eviction never disposes painters' images. */
@OptIn(ExperimentalAtomicApi::class)
internal class ImageBitmapMemoryCache(
    private val maxSizeBytes: Long = 48L * 1024 * 1024,
    override val maxSize: Int = 100,
) : Cache<Any, ImageBitmap> {
    init {
        require(maxSizeBytes >= 0) { "Image cache byte limit must not be negative" }
        require(maxSize >= 0) { "Image cache entry limit must not be negative" }
    }

    // Published states are never mutated. Reads promote entries atomically with background writes.
    private val state = AtomicReference(State())

    override val size: Int get() = state.load().entries.size

    override fun get(key: Any): ImageBitmap? {
        while (true) {
            val current = state.load()
            val index = current.entries.indexOfFirst { it.key == key }
            if (index == -1) return null
            val entry = current.entries[index]
            if (index == current.entries.lastIndex) return entry.bitmap
            val entries = current.entries.toMutableList().apply {
                removeAt(index)
                add(entry)
            }
            if (state.compareAndSet(current, State(entries.toList(), current.bytes))) {
                return entry.bitmap
            }
        }
    }

    override fun set(key: Any, value: ImageBitmap) {
        val bytes = value.estimatedBytes()
        while (true) {
            val current = state.load()
            val entries = current.entries.toMutableList()
            val previousIndex = entries.indexOfFirst { it.key == key }
            var retainedBytes = current.bytes
            if (previousIndex != -1) retainedBytes -= entries.removeAt(previousIndex).bytes

            if (maxSize > 0 && bytes <= maxSizeBytes) {
                // Trim before adding to keep accounting safe even for a Long.MAX_VALUE budget.
                while (entries.isNotEmpty() &&
                    (entries.size >= maxSize || retainedBytes > maxSizeBytes - bytes)
                ) {
                    retainedBytes -= entries.removeAt(0).bytes
                }
                entries.add(Entry(key, value, bytes))
                retainedBytes += bytes
            }
            if (state.compareAndSet(current, State(entries.toList(), retainedBytes))) return
        }
    }

    override fun remove(key: Any): Boolean {
        while (true) {
            val current = state.load()
            val index = current.entries.indexOfFirst { it.key == key }
            if (index == -1) return false
            val entries = current.entries.toMutableList()
            val removed = entries.removeAt(index)
            if (state.compareAndSet(current, State(entries.toList(), current.bytes - removed.bytes))) {
                return true
            }
        }
    }

    override fun clear() = state.store(State())

    private class Entry(val key: Any, val bitmap: ImageBitmap, val bytes: Long)
    private class State(val entries: List<Entry> = emptyList(), val bytes: Long = 0)
}

private fun ImageBitmap.estimatedBytes(): Long {
    val pixels = width.toLong().coerceAtLeast(1) * height.toLong().coerceAtLeast(1)
    return if (pixels > Long.MAX_VALUE / 4) Long.MAX_VALUE else pixels * 4
}
