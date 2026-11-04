package com.coffeepeek.feature.shelf.impl.ui.compose.component

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublicShelfPhotoBoundsTest {
    @Test fun ordinaryPngAndJpegCardsFitTheRasterBudget() {
        assertTrue(publicShelfPhotoFits(png(720, 960)))
        assertTrue(publicShelfPhotoFits(jpeg(720, 960)))
    }

    @Test fun compressedCardsCannotRequestAnUnboundedPixelAllocation() {
        assertFalse(publicShelfPhotoFits(png(4000, 4000)))
        assertFalse(publicShelfPhotoFits(jpeg(4000, 4000)))
        assertFalse(publicShelfPhotoFits(png(Int.MAX_VALUE, 2)))
        assertFalse(publicShelfPhotoFits(png(0, 100)))
    }

    @Test fun malformedAndUnknownImagesAreRejectedBeforeDecoding() {
        assertFalse(publicShelfPhotoFits(byteArrayOf()))
        assertFalse(publicShelfPhotoFits(byteArrayOf(-1, -40, -1, -64, 0, 0)))
        assertFalse(publicShelfPhotoFits("WEBP".encodeToByteArray()))
        assertFalse(publicShelfPhotoFits(png(720, 960).copyOf(20)))
    }

    @Test fun encodedAndHighBitDepthImagesCannotExceedTheBudget() {
        assertFalse(publicShelfPhotoFits(png(720, 960).copyOf(1024 * 1024 + 1)))
        assertFalse(publicShelfPhotoFits(png(720, 960).apply { this[24] = 16 }))
        assertFalse(publicShelfPhotoFits(jpeg(720, 960).apply { this[6] = 12 }))
    }

    private fun png(width: Int, height: Int): ByteArray = ByteArray(33).apply {
        byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10).copyInto(this)
        number(8, 13)
        "IHDR".encodeToByteArray().copyInto(this, 12)
        number(16, width)
        number(20, height)
        this[24] = 8
    }

    private fun jpeg(width: Int, height: Int): ByteArray = byteArrayOf(-1, -40, -1, -64, 0, 7, 8,
        (height shr 8).toByte(), height.toByte(), (width shr 8).toByte(), width.toByte())

    private fun ByteArray.number(offset: Int, value: Int) {
        repeat(4) { this[offset + it] = (value ushr ((3 - it) * 8)).toByte() }
    }
}
