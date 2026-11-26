package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionImageMemoryCacheTest {
    @Test fun cachedBytesSurviveCallerErasureWithoutRetainingTheTransferBuffer() = runTest {
        val cache = CollectionImageMemoryCache(ImageAccount(), backgroundScope)
        val transfer = byteArrayOf(1, 2, 3)
        val first = cache.read("cover:revision-1") { transfer }
        first?.fill(9)
        val second = cache.read("cover:revision-1") { error("Unexpected image transfer") }
        assertContentEquals(byteArrayOf(1, 2, 3), second)
        assertContentEquals(byteArrayOf(0, 0, 0), transfer)
    }

    @Test fun aNewImmutableMediaRevisionLoadsItsOwnBytes() = runTest {
        val cache = CollectionImageMemoryCache(ImageAccount(), backgroundScope)
        cache.read("collection:cover-v1") { byteArrayOf(1) }
        val changed = cache.read("collection:cover-v2") { byteArrayOf(2) }
        assertContentEquals(byteArrayOf(2), changed)
        assertContentEquals(byteArrayOf(1), cache.read("collection:cover-v1") { error("Unexpected transfer") })
    }

    @Test fun missingAndFailedMediaNeverPreventALaterSuccessfulRead() = runTest {
        val cache = CollectionImageMemoryCache(ImageAccount(), backgroundScope)
        assertNull(cache.read("missing") { null })
        assertFailsWith<IllegalArgumentException> {
            cache.read("failed") { throw IllegalArgumentException("Unavailable media") }
        }
        assertContentEquals(byteArrayOf(1), cache.read("missing") { byteArrayOf(1) })
        assertContentEquals(byteArrayOf(2), cache.read("failed") { byteArrayOf(2) })
    }

    @Test fun aNewLoginForTheSameUserCannotReuseThePreviousLifetimesMedia() = runTest {
        val account = ImageAccount()
        val cache = CollectionImageMemoryCache(account, backgroundScope)
        cache.read("cover") { byteArrayOf(1) }
        account.scopes.value = "owner:2"
        val next = cache.read("cover") { byteArrayOf(2) }
        account.scopes.value = null
        val guest = cache.read("cover") { byteArrayOf(3) }
        assertContentEquals(byteArrayOf(2), next)
        assertContentEquals(byteArrayOf(3), guest)
    }

    @Test fun aLateOldLoginResponseIsErasedAndCannotFillTheNewLoginsCache() = runTest {
        val account = ImageAccount()
        val cache = CollectionImageMemoryCache(account, backgroundScope)
        val gate = CompletableDeferred<Unit>()
        val old = byteArrayOf(1, 2)
        val request = async {
            cache.read("cover") {
                withContext(NonCancellable) {
                    gate.await()
                    old
                }
            }
        }
        runCurrent()
        account.scopes.value = "owner:2"
        assertContentEquals(byteArrayOf(3), cache.read("cover") { byteArrayOf(3) })
        gate.complete(Unit)
        assertFailsWith<CancellationException> { request.await() }
        assertContentEquals(byteArrayOf(0, 0), old)
        assertContentEquals(byteArrayOf(3), cache.read("cover") { error("New login cache was replaced") })
    }

    @Test fun encodedMediaExpiresAndLeastRecentlyUsedBytesAreBounded() = runTest {
        val clock = TestTimeSource()
        val cache = CollectionImageMemoryCache(ImageAccount(), backgroundScope, clock, maxBytes = 4, maxEntries = 2)
        cache.read("one") { byteArrayOf(1, 1) }
        cache.read("two") { byteArrayOf(2, 2) }
        cache.read("one") { error("Unexpected transfer") }
        cache.read("three") { byteArrayOf(3, 3) }
        var reads = 0
        cache.read("two") {
            reads++
            byteArrayOf(2, 2)
        }
        clock += 11.minutes
        cache.read("two") {
            reads++
            byteArrayOf(4, 4)
        }
        assertEquals(2, reads)
    }
}

private class ImageAccount : CollectionsAccount {
    val scopes = MutableStateFlow<String?>("owner:1")
    override val identities = scopes
    override val mediaScopes = scopes
    override fun currentId() = scopes.value?.substringBefore(':')
    override fun mediaScope() = scopes.value
    override suspend fun loadId() = currentId()
}
