package com.coffeepeek.core.media.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImageBitmapMemoryCacheTest {
    @Test
    fun decodedPixelBudgetEvictsOldestImagesBeforeEntryLimit() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 32)
        cache["first"] = bitmap()
        val second = bitmap()
        cache["second"] = second
        val third = bitmap()
        cache["third"] = third

        assertEquals(100, cache.maxSize)
        assertEquals(2, cache.size)
        assertNull(cache["first"])
        assertSame(second, cache["second"])
        assertSame(third, cache["third"])
    }

    @Test
    fun readingAnImagePromotesItBeforeEviction() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 32)
        val first = bitmap()
        cache["first"] = first
        cache["second"] = bitmap()
        assertSame(first, cache["first"])
        cache["third"] = bitmap()

        assertNull(cache["second"])
        assertSame(first, cache["first"])
    }

    @Test
    fun entryLimitAlsoAppliesToSmallImages() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 1024, maxSize = 2)
        repeat(3) { cache[it] = bitmap() }

        assertEquals(2, cache.maxSize)
        assertEquals(2, cache.size)
        assertNull(cache[0])
        assertTrue(cache.remove(1))
        assertTrue(cache.remove(2))
    }

    @Test
    fun replacementSubtractsPreviousBytesAndPromotesTheKey() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 48)
        cache["first"] = bitmap(4, 2) // 32 bytes
        val second = bitmap()
        cache["second"] = second
        val replacement = bitmap()
        cache["first"] = replacement
        cache["third"] = bitmap()

        assertEquals(3, cache.size)
        assertSame(second, cache["second"])
        assertSame(replacement, cache["first"])
        cache["fourth"] = bitmap()
        assertNull(cache["third"])
    }

    @Test
    fun replacingAnImageMakesItsKeyMostRecentlyUsed() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 32)
        cache["first"] = bitmap()
        cache["second"] = bitmap()
        val replacement = bitmap()
        cache["first"] = replacement
        cache["third"] = bitmap()

        assertNull(cache["second"])
        assertSame(replacement, cache["first"])
    }

    @Test
    fun oversizedReplacementRemovesTheOldValueWithoutEvictingOtherKeys() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 32)
        cache["first"] = bitmap()
        val second = bitmap()
        cache["second"] = second
        cache["first"] = bitmap(4, 4)
        cache["huge"] = bitmap(Int.MAX_VALUE, Int.MAX_VALUE)

        assertEquals(1, cache.size)
        assertNull(cache["first"])
        assertNull(cache["huge"])
        assertSame(second, cache["second"])
    }

    @Test
    fun removalFreesTheByteBudgetAndReportsMissingKeys() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 32)
        cache["first"] = bitmap()
        val second = bitmap()
        cache["second"] = second

        assertTrue(cache.remove("first"))
        assertFalse(cache.remove("first"))
        cache["third"] = bitmap()
        assertEquals(2, cache.size)
        assertSame(second, cache["second"])
    }

    @Test
    fun clearingReleasesAllEntriesAndResetsAccounting() {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 32)
        cache["first"] = bitmap(4, 2)
        cache.clear()
        assertEquals(0, cache.size)
        assertNull(cache["first"])

        cache["second"] = bitmap()
        cache["third"] = bitmap()
        assertEquals(2, cache.size)
    }

    @Test
    fun zeroLimitsDisableRetentionAndNegativeLimitsAreRejected() {
        val noBytes = ImageBitmapMemoryCache(maxSizeBytes = 0)
        val noEntries = ImageBitmapMemoryCache(maxSize = 0)
        noBytes["image"] = bitmap()
        noEntries["image"] = bitmap()

        assertEquals(0, noBytes.size)
        assertEquals(0, noEntries.size)
        assertFailsWith<IllegalArgumentException> { ImageBitmapMemoryCache(maxSizeBytes = -1) }
        assertFailsWith<IllegalArgumentException> { ImageBitmapMemoryCache(maxSize = -1) }
    }

    @Test
    fun concurrentWritesAndRemovalsDoNotLoseOtherEntries() = runTest {
        val cache = ImageBitmapMemoryCache(maxSizeBytes = 256, maxSize = 64)
        coroutineScope {
            repeat(8) { worker ->
                launch(Dispatchers.Default) {
                    repeat(8) { index ->
                        val key = worker * 8 + index
                        cache[key] = bitmap(1, 1)
                        assertTrue(cache[key] != null)
                    }
                }
            }
        }
        assertEquals(64, cache.size)
        coroutineScope {
            repeat(8) { worker ->
                launch(Dispatchers.Default) {
                    repeat(4) { index -> assertTrue(cache.remove(worker * 8 + index)) }
                }
            }
        }
        assertEquals(32, cache.size)
        repeat(8) { worker ->
            repeat(4) { index -> assertTrue(cache[worker * 8 + index + 4] != null) }
        }
    }

    private fun bitmap(pixelWidth: Int = 2, pixelHeight: Int = 2): ImageBitmap = object : ImageBitmap {
        override val width = pixelWidth
        override val height = pixelHeight
        override val colorSpace: ColorSpace get() = ColorSpaces.Srgb
        override val hasAlpha = true
        override val config = ImageBitmapConfig.Argb8888

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) = error("Cache must not read or allocate image pixels")

        override fun prepareToDraw() = error("Cache must not upload or dispose image pixels")
    }
}
