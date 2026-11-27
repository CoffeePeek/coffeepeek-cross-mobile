package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Copies belong to their callers; retained collection media never outlives its account scope. */
internal class CollectionImageMemoryCache(
    private val account: CollectionsAccount,
    scope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val maxBytes: Int = 8 * 1024 * 1024,
    private val maxEntries: Int = 24,
) {
    private data class Entry(val bytes: ByteArray, val stored: TimeMark)
    private val mutex = Mutex()
    private val entries = linkedMapOf<String, Entry>()
    private var ownerScope = account.mediaScope()
    private var retainedBytes = 0
    private var enabled = true

    init {
        require(maxBytes >= 0 && maxEntries >= 0)
        scope.launch {
            try { account.mediaScopes.collect { mutex.withLock { synchronizeScope() } } }
            catch (cancelled: CancellationException) { disable(); throw cancelled }
            catch (_: Exception) { disable() }
        }
    }

    suspend fun read(key: String, load: suspend () -> ByteArray?): ByteArray? {
        currentCoroutineContext().ensureActive()
        val owner = account.mediaScope()
        cached(key, owner)?.let { return checked(it, owner) }
        val bytes = load() ?: return null
        try {
            currentCoroutineContext().ensureActive()
            return checked(remember(key, owner, bytes), owner)
        } finally { bytes.fill(0) }
    }

    private suspend fun checked(bytes: ByteArray, owner: String?): ByteArray {
        try {
            currentCoroutineContext().ensureActive()
            if (owner != account.mediaScope()) throw CancellationException("Collection media account changed")
            return bytes
        } catch (cancelled: CancellationException) { bytes.fill(0); throw cancelled }
    }

    private suspend fun cached(key: String, owner: String?): ByteArray? = mutex.withLock {
        synchronizeScope()
        if (!enabled || owner != ownerScope) return@withLock null
        val entry = entries.remove(key) ?: return@withLock null
        if (entry.stored.elapsedNow() >= 10.minutes) {
            retainedBytes -= entry.bytes.size
            entry.bytes.fill(0)
            return@withLock null
        }
        entries[key] = entry
        entry.bytes.copyOf()
    }

    private suspend fun remember(key: String, owner: String?, bytes: ByteArray): ByteArray = mutex.withLock {
        synchronizeScope()
        if (owner != ownerScope) throw CancellationException("Collection media account changed")
        if (enabled && bytes.isNotEmpty() && bytes.size <= maxBytes && maxEntries > 0) {
            entries.remove(key)?.let { retainedBytes -= it.bytes.size; it.bytes.fill(0) }
            trim(bytes.size)
            entries[key] = Entry(bytes.copyOf(), timeSource.markNow())
            retainedBytes += bytes.size
        }
        bytes.copyOf()
    }

    private fun trim(incoming: Int) {
        while (entries.isNotEmpty() && (entries.size >= maxEntries || retainedBytes > maxBytes - incoming)) {
            val entry = entries.remove(entries.keys.first()) ?: continue
            retainedBytes -= entry.bytes.size
            entry.bytes.fill(0)
        }
    }

    private fun synchronizeScope() {
        val current = account.mediaScope()
        if (current != ownerScope) { clear(); ownerScope = current }
    }

    private fun clear() {
        entries.values.forEach { it.bytes.fill(0) }
        entries.clear()
        retainedBytes = 0
    }

    private suspend fun disable() = withContext(NonCancellable) {
        mutex.withLock { clear(); enabled = false }
    }
}
