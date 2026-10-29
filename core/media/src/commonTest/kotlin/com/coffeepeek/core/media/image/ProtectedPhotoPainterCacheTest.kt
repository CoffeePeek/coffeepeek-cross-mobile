package com.coffeepeek.core.media.image

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ProtectedPhotoPainterCacheTest {
    private class Clock(var wall: Long = 1000, var mono: Long = 0) : PhotoCacheClock {
        override fun wallMillis() = wall
        override fun monotonicMillis() = mono
    }
    private val session = Any()
    private fun url(photo: String = "photo", purpose: String = "feed") =
        "https://gateway.example/api/v1/check-ins/visit/photos/$photo/variants/source/1/$purpose.png"
    private fun painter(size: Size = Size(2f, 2f)): Painter = object : Painter() {
        override val intrinsicSize = size
        override fun DrawScope.onDraw() = error("Cache must not draw or upload retained painters")
    }
    private fun permission() = PhotoPermission(monotonicDeadline = 20_000, wallDeadline = 21_000)

    @Test fun reopeningVariantReusesExactPainterWithoutExtendingItsPermission() {
        val clock = Clock()
        val cache = ProtectedPhotoPainterCache(clock, { true })
        val painter = painter()
        cache.store(url(), session, painter, permission())

        clock.mono = 5_000
        clock.wall = 6_000
        val restored = cache.get(url(), session)!!
        assertSame(painter, restored.painter)
        assertEquals(15_000L, restored.permission.remaining(clock))
        clock.mono = 20_000
        assertNull(cache.get(url(), session))
    }

    @Test fun forwardWallClockJumpAndMonotonicTimeIndependentlyRevokePainters() {
        for (useWall in listOf(false, true)) {
            val clock = Clock()
            val cache = ProtectedPhotoPainterCache(clock, { true })
            cache.store(url(), session, painter(), permission())
            if (useWall) clock.wall = 21_001 else clock.mono = 20_001
            assertNull(cache.get(url(), session))
            // Returning the clock to its old value cannot resurrect an evicted grant.
            clock.wall = 1000
            clock.mono = 0
            assertNull(cache.get(url(), session))
        }
    }

    @Test fun sessionRevocationAndExplicitInvalidationDiscardOldPainters() {
        var active = session
        val cache = ProtectedPhotoPainterCache(Clock(), { it === active })
        cache.store(url(), session, painter(), permission())
        active = Any()
        assertNull(cache.get(url(), session))
        assertNull(cache.get(url(), active))

        val freshPainter = painter()
        cache.store(url(), active, freshPainter, permission())
        cache.discardInvalidScopes { false }
        assertNull(cache.get(url(), active))
    }

    @Test fun noStoreAndOnlineOnlyPresentationCannotBecomeReopenHits() {
        val cache = ProtectedPhotoPainterCache(Clock(), { true })
        cache.store(url(), session, painter(), permission())
        cache.store(url(), session, painter(), reusablePermission = null)
        assertNull(cache.get(url(), session))
        cache.store("https://gateway.example/api/v1/check-ins/visit/photos/photo", session, painter(), permission())
        assertNull(cache.get("https://gateway.example/api/v1/check-ins/visit/photos/photo", session))
    }

    @Test fun fullVariantIdentityAndSessionScopeStaySeparate() {
        val cache = ProtectedPhotoPainterCache(Clock(), { true })
        val first = painter()
        val detail = painter()
        val anotherSession = Any()
        cache.store(url(), session, first, permission())
        cache.store(url(purpose = "detail"), session, detail, permission())
        assertSame(first, cache.get(url(), session)?.painter)
        assertSame(detail, cache.get(url(purpose = "detail"), session)?.painter)
        assertNull(cache.get(url(), anotherSession))
        assertNull(cache.get(url().replace("/source/", "/another-source/"), session))
    }

    @Test fun pixelBudgetAndReadPromotionEvictLeastRecentlyUsedPainter() {
        val cache = ProtectedPhotoPainterCache(Clock(), { true }, maxBytes = 32)
        val first = painter()
        val third = painter()
        cache.store(url("first"), session, first, permission())
        cache.store(url("second"), session, painter(), permission())
        assertSame(first, cache.get(url("first"), session)?.painter)
        cache.store(url("third"), session, third, permission())
        assertNull(cache.get(url("second"), session))
        assertSame(first, cache.get(url("first"), session)?.painter)
        assertSame(third, cache.get(url("third"), session)?.painter)
    }

    @Test fun entryBudgetUnknownDimensionsAndOversizedReplacementAreBounded() {
        val cache = ProtectedPhotoPainterCache(Clock(), { true }, maxBytes = 64, maxEntries = 2)
        repeat(3) { cache.store(url(it.toString()), session, painter(), permission()) }
        assertNull(cache.get(url("0"), session))
        cache.store(url("2"), session, painter(Size(100f, 100f)), permission())
        assertNull(cache.get(url("2"), session))
        assertEquals(permission(), cache.get(url("1"), session)?.permission)
        for (size in listOf(Size.Unspecified, Size.Zero, Size(Float.POSITIVE_INFINITY, 2f))) {
            cache.store(url("unknown"), session, painter(size), permission())
            assertNull(cache.get(url("unknown"), session))
        }
    }

    @Test fun concurrentDecodesDoNotLoseDifferentVariants() = runTest {
        val cache = ProtectedPhotoPainterCache(Clock(), { true }, maxBytes = 1024, maxEntries = 64)
        val painters = List(32) { painter() }
        coroutineScope {
            painters.forEachIndexed { index, painter ->
                launch(Dispatchers.Default) { cache.store(url(index.toString()), session, painter, permission()) }
            }
        }
        painters.forEachIndexed { index, painter -> assertSame(painter, cache.get(url(index.toString()), session)?.painter) }
    }
}
