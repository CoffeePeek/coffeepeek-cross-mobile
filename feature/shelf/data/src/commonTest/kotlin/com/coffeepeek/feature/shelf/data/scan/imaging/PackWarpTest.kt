package com.coffeepeek.feature.shelf.data.scan.imaging

import kotlin.test.*

class PackWarpTest {
    private val width = 8
    private val height = 6
    private val photo = IntArray(width * height) { argb((it % width) * 30f, (it / width) * 40f, 100f) }

    @Test fun theWholePhotoAtItsOwnSizeComesBackUnchanged() {
        val quad = Quad(doubleArrayOf(0.0, 0.0, 8.0, 0.0, 8.0, 6.0, 0.0, 6.0))
        assertContentEquals(photo, rectify(photo, width, height, quad, width, height))
    }

    @Test fun aQuarterOfThePhotoIsCutOutUpright() {
        val quad = Quad(doubleArrayOf(4.0, 0.0, 8.0, 0.0, 8.0, 3.0, 4.0, 3.0))
        val out = rectify(photo, width, height, quad, 4, 3)
        assertEquals(photo[4], out[0])
        assertEquals(photo[2 * width + 7], out[2 * 4 + 3])
    }

    @Test fun aShrunkPhotoAveragesWhatIsBehindEachPixel() {
        val quad = Quad(doubleArrayOf(0.0, 0.0, 8.0, 0.0, 8.0, 6.0, 0.0, 6.0))
        val out = rectify(photo, width, height, quad, 4, 3)
        assertEquals(argb(15f, 20f, 100f), out[0])
    }

    @Test fun whatLiesOutsideThePhotoIsBlack() {
        val quad = Quad(doubleArrayOf(-8.0, 0.0, 0.0, 0.0, 0.0, 6.0, -8.0, 6.0))
        assertTrue(rectify(photo, width, height, quad, 4, 3).all { it == argb(0f, 0f, 0f) })
    }
}
