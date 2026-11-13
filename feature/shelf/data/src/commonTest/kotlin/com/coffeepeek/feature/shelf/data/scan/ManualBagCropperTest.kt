package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.data.scan.imaging.Quad
import com.coffeepeek.feature.shelf.domain.model.BagCropBounds
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManualBagCropperTest {
    @Test fun selectedRectangleIsEncodedFromTheOriginalPhoto() = runTest {
        val imaging = CropImaging()
        val photo = byteArrayOf(3, 4)
        val result = manualBagCropper(imaging).crop(photo, BagCropBounds(0.2f, 0.1f, 0.7f, 0.8f)).getOrThrow()
        assertTrue(imaging.received === photo)
        assertEquals(5, result.width)
        assertEquals(7, result.height)
        assertEquals(listOf(12, 13, 14, 15, 16), imaging.encoded!!.pixels.take(5))
        assertEquals("image/jpeg", result.card.contentType)
    }

    @Test fun oversizedJpegIsReencodedWithinTheShelfPreviewLimit() = runTest {
        val imaging = CropImaging(oversizedAtQuality = 85)
        val result = manualBagCropper(imaging).crop(byteArrayOf(1), BagCropBounds.Default).getOrThrow()
        assertEquals(listOf(85, 75), imaging.qualities)
        assertTrue(result.card.bytes.size <= 1024 * 1024)
    }

    @Test fun invalidBoundsCannotCreateACard() = runTest {
        val imaging = CropImaging()
        val result = manualBagCropper(imaging).crop(byteArrayOf(1), BagCropBounds(0.8f, 0f, 0.2f, 1f))
        assertTrue(result.isFailure)
        assertEquals(null, imaging.received)
    }
}

private class CropImaging(private val oversizedAtQuality: Int? = null) : PackImaging {
    var received: ByteArray? = null
    var encoded: PackImage? = null
    val qualities = mutableListOf<Int>()

    override fun decode(photo: ByteArray, maxSide: Int): PackImage {
        received = photo
        return PackImage(10, 10, IntArray(100) { it })
    }

    override fun scaledDown(image: PackImage, maxSide: Int) = image
    override fun rectified(image: PackImage, quad: Quad, width: Int, height: Int) = error("Scanner must not be used")

    override fun jpeg(image: PackImage, quality: Int): ByteArray {
        encoded = image
        qualities += quality
        return ByteArray(if (quality == oversizedAtQuality) 1024 * 1024 + 1 else 3)
    }
}
