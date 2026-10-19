package com.coffeepeek.admin.utils.image

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProtectedPhotoCacheTest {
    private val base = 1_792_324_800_000L // Sun, 18 Oct 2026 12:00:00 GMT
    private val date = "Sun, 18 Oct 2026 12:00:00 GMT"
    private class Clock(var wall: Long, var mono: Long = 0) : PhotoCacheClock {
        override fun wallMillis() = wall
        override fun monotonicMillis() = mono
        fun advance(ms: Long) { wall += ms; mono += ms }
    }
    private fun headers(policy: String = "private, max-age=20, must-revalidate", dateValue: String? = date, age: String? = null, etag: String? = "\"variant-a\"") = Headers.build {
        append(HttpHeaders.CacheControl, policy)
        dateValue?.let { append(HttpHeaders.Date, it) }
        age?.let { append(HttpHeaders.Age, it) }
        etag?.let { append(HttpHeaders.ETag, it) }
    }
    private fun key(scope: Any = this, path: String = "photo") = PhotoCacheKey("/api/v1/check-ins/visit/photos/$path/variants/source/1/feed.png", scope)
    private fun response(h: Headers = headers(), status: Int = 200, bytes: ByteArray = byteArrayOf(1, 2)) = PhotoTransfer(status, h, bytes)

    @Test fun rfcAgeUsesApparentAgeAndFullRoundTripEvenWhenDateIsNewer() {
        val cases = listOf(
            Triple(headers(), 2000L, 18_000L),
            Triple(headers(dateValue = "Sun, 18 Oct 2026 12:00:01 GMT"), 2000L, 18_000L),
            Triple(headers(dateValue = "Sun, 18 Oct 2026 11:59:50 GMT"), 2000L, 8_000L),
            Triple(headers(age = "10"), 2000L, 8_000L),
            Triple(headers(age = "19"), 2000L, 0L),
            Triple(headers(dateValue = "Sun, 18 Oct 2026 12:01:40 GMT"), 2000L, 18_000L),
            Triple(headers("private, max-age=999"), 2000L, 18_000L),
            Triple(headers(dateValue = null), 2000L, 0L),
            Triple(headers(dateValue = "invalid"), 2000L, 0L),
        )
        cases.forEach { (h, delay, expected) ->
            assertEquals(expected, PhotoCachePolicy.read(h, 0, delay, base + delay).freshMillis)
        }
    }

    @Test fun malformedOrConflictingPolicyNeverCreatesPositiveReuse() {
        for (policy in listOf("private, max-age=20, max-age=10", "private, max-age=-1", "private, max-age=foo", "private, max-age=999999999999999999999", "private, max-age=20, no-store")) {
            val actual = PhotoCachePolicy.read(headers(policy), 0, 0, base)
            assertFalse(actual.stores)
            assertEquals(0L, actual.freshMillis)
        }
        for (age in listOf("-1", "foo", "999999999999999999999")) assertFalse(PhotoCachePolicy.read(headers(age = age), 0, 0, base).stores)
        assertFalse(PhotoCachePolicy.read(headersOf(HttpHeaders.CacheControl to listOf("private,max-age=20"), HttpHeaders.Vary to listOf("*")), 0, 0, base).stores)
    }

    @Test fun freshBytesReuseOnlyUntilTheOriginalDeadlineAndThen304KeepsExactBytes() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        var calls = 0
        val original = byteArrayOf(4, 7)
        val first = cache.read(key(), base + 100_000, { true }) { assertNull(it); calls++; response(bytes = original) }
        clock.advance(5_000)
        val second = cache.read(key(), base + 100_000, { true }) { error("must reuse") }
        assertSame(first.bytes, second.bytes)
        assertEquals(15_000L, second.permission.remaining(clock))
        clock.advance(16_000)
        val third = cache.read(key(), base + 100_000, { true }) {
            assertEquals("\"variant-a\"", it); calls++
            response(headers(dateValue = "Sun, 18 Oct 2026 12:00:21 GMT"), status = 304)
        }
        assertSame(original, third.bytes)
        assertEquals(2, calls)
    }

    @Test fun responseBodyAndDecodeTimeCannotExtendPositiveOrZeroFreshnessPermission() = runTest {
        for (policy in listOf("private,max-age=20", "private,max-age=0", "private,no-store")) {
            val clock = Clock(base)
            val cache = ProtectedPhotoCache(backgroundScope, clock)
            val result = cache.read(key(), base + 100_000, { true }) { clock.advance(5_000); response(headers(policy)) }
            assertEquals(15_000L, result.permission.remaining(clock))
            clock.advance(17_000)
            assertEquals(0L, result.permission.remaining(clock))
        }
    }

    @Test fun positiveResponseThatExpiredInFlightCannotPublish() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        assertFailsWith<IllegalStateException> {
            cache.read(key(), base + 100_000, { true }) { clock.advance(2_000); response(headers(age = "19")) }
        }
    }

    @Test fun zeroFreshnessRetainsValidatorOnlyForOnlineRevalidation() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        cache.read(key(), base + 100_000, { true }) { response(headers("private,max-age=0,must-revalidate")) }
        val again = cache.read(key(), base + 100_000, { true }) {
            assertEquals("\"variant-a\"", it)
            response(headers("private,max-age=0,must-revalidate"), status = 304)
        }
        assertContentEquals(byteArrayOf(1, 2), again.bytes)
    }

    @Test fun noCacheAuthorizesOnlyCurrentOnlineDisplayWithinAgedPermission() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        repeat(2) { count ->
            val result = cache.read(key(), base + 100_000, { true }) {
                assertEquals(if (count == 0) null else "\"variant-a\"", it)
                clock.advance(1000)
                response(headers("private,max-age=20,no-cache"))
            }
            assertEquals(19_000L - count * 1000L, result.permission.remaining(clock))
        }
    }

    @Test fun missingDateRetainsBytesButNeverPositiveFreshness() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        cache.read(key(), base + 100_000, { true }) { response(headers(dateValue = null)) }
        cache.read(key(), base + 100_000, { true }) {
            assertEquals("\"variant-a\"", it)
            response(headers(dateValue = null), status = 304)
        }
    }

    @Test fun noStore200EvictsBytesAndValidator() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        cache.read(key(), base + 100_000, { true }) { response(headers("private,max-age=0")) }
        cache.read(key(), base + 100_000, { true }) { response(headers("private,no-store")) }
        cache.read(key(), base + 100_000, { true }) { assertNull(it); response() }
    }

    @Test fun unexpected304NeverRevivesBytesAndEvictsCandidate() = runTest {
        for (badHeaders in listOf(headers(etag = "\"wrong\""), headers(etag = null), headers("private,no-store"), Headers.Empty)) {
            val clock = Clock(base)
            val cache = ProtectedPhotoCache(backgroundScope, clock)
            cache.read(key(), base + 100_000, { true }) { response(headers("private,max-age=0")) }
            assertFailsWith<IllegalStateException> {
                cache.read(key(), base + 100_000, { true }) { response(badHeaders, status = 304) }
            }
            cache.read(key(), base + 100_000, { true }) { assertNull(it); response() }
        }
    }

    @Test fun repeated304WithOldDateDoesNotSlideThePermissionWindow() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        cache.read(key(), base + 100_000, { true }) { response() }
        clock.advance(21_000)
        assertFailsWith<IllegalStateException> {
            cache.read(key(), base + 100_000, { true }) { response(status = 304) }
        }
    }

    @Test fun new304UsesNewAuthorizationPolicyInsteadOfLatchingOldZeroOrShortDuration() = runTest {
        for (initialSeconds in listOf(0, 1, 2)) {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        cache.read(key(), base + 100_000, { true }) { response(headers("private,max-age=$initialSeconds")) }
        clock.advance(3_000)
        val validated = cache.read(key(), base + 100_000, { true }) {
            response(headers(dateValue = "Sun, 18 Oct 2026 12:00:03 GMT"), status = 304)
        }
        assertEquals(20_000L, validated.permission.remaining(clock))
        }
    }

    @Test fun jwtAndBothClocksBoundAllReuse() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        val result = cache.read(key(), base + 5_000, { true }) { clock.advance(2_000); response() }
        assertEquals(3_000L, result.permission.remaining(clock))
        clock.wall = base + 1_000
        clock.mono = 4_000
        assertEquals(1_000L, result.permission.remaining(clock))
        clock.wall = base + 6_000
        assertEquals(0L, result.permission.remaining(clock))
        assertFailsWith<IllegalStateException> { cache.read(key(), base + 5_000, { true }) { error("JWT expired") } }
    }

    @Test fun failedRevalidationNeverFallsBackToStaleAndEvictsValidator() = runTest {
        for (status in listOf(401, 403, 404, 500)) {
            val clock = Clock(base)
            val cache = ProtectedPhotoCache(backgroundScope, clock)
            cache.read(key(), base + 100_000, { true }) { response(headers("private,max-age=0")) }
            assertFailsWith<IllegalStateException> { cache.read(key(), base + 100_000, { true }) { response(status = status) } }
            cache.read(key(), base + 100_000, { true }) { assertNull(it); response() }
        }
    }

    @Test fun accountOrLocalInvalidationDiscardsCompletedOldWork() = runTest { supervisorScope {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        var current = true
        val finish = CompletableDeferred<Unit>()
        val old = async { cache.read(key(), base + 100_000, { current }) { finish.await(); response() } }
        runCurrent()
        current = false
        finish.complete(Unit)
        assertFailsWith<IllegalStateException> { old.await() }
    } }

    @Test fun cancelledNonCancellableOldTransferCannotOverwriteOrEvictReplacement() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        val finishOld = CompletableDeferred<Unit>()
        val old = async { cache.read(key(), base + 100_000, { true }) {
            withContext(NonCancellable) { finishOld.await() }
            response(bytes = byteArrayOf(1))
        } }
        runCurrent(); old.cancelAndJoin()
        cache.read(key(), base + 100_000, { true }) { response(bytes = byteArrayOf(9)) }
        finishOld.complete(Unit); runCurrent()
        val current = cache.read(key(), base + 100_000, { true }) { error("replacement must stay cached") }
        assertContentEquals(byteArrayOf(9), current.bytes)
    }

    @Test fun evictedCandidateCannotBeRevivedByDelayed304() = runTest { supervisorScope {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock, maxEntries = 1)
        cache.read(key(path = "a"), base + 100_000, { true }) { response(headers("private,max-age=0")) }
        val finish = CompletableDeferred<Unit>()
        val old = async { cache.read(key(path = "a"), base + 100_000, { true }) {
            finish.await(); response(headers("private,max-age=0"), status = 304)
        } }
        runCurrent()
        cache.read(key(path = "b"), base + 100_000, { true }) { response() }
        finish.complete(Unit)
        assertFailsWith<IllegalStateException> { old.await() }
    } }

    @Test fun lruHonorsEntryAndByteBoundsAndOversizedBytesAreNeverRetained() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock, maxEntries = 2, maxBytes = 4)
        suspend fun load(path: String, expectedTag: String? = null, size: Int = 2) {
            cache.read(key(path = path), base + 100_000, { true }) {
                assertEquals(expectedTag, it); response(bytes = ByteArray(size) { 3 })
            }
        }
        load("a"); load("b")
        cache.read(key(path = "a"), base + 100_000, { true }) { error("a is fresh") }
        load("c")
        load("b") // least recently used b was evicted
        load("large", size = 5)
        load("large", size = 5) // exceeds byte cap, no validator
    }

    @Test fun duplicateSubscribersShareTransferAndLastCancellationStopsIt() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        var cancelled = false
        suspend fun transfer(tag: String?): PhotoTransfer { calls++; try { finish.await() } finally { cancelled = !finish.isCompleted }; return response() }
        val first = async { cache.read(key(), base + 100_000, { true }, ::transfer) }
        val second = async { cache.read(key(), base + 100_000, { true }, ::transfer) }
        runCurrent()
        assertEquals(1, calls)
        first.cancelAndJoin()
        assertTrue(second.isActive)
        second.cancelAndJoin(); runCurrent()
        assertTrue(cancelled)
        cache.read(key(), base + 100_000, { true }) { assertNull(it); response() }
    }

    @Test fun distinctSessionsAndEntireVariantUrlsNeverCoalesce() = runTest {
        val clock = Clock(base)
        val cache = ProtectedPhotoCache(backgroundScope, clock)
        for (k in listOf(key(Any()), key(Any()), key(path = "source-v2"), key().copy(url = key().url.replace("/1/feed.png", "/2/detail.webp")))) {
            cache.read(k, base + 100_000, { true }) { assertNull(it); response() }
        }
    }
}
