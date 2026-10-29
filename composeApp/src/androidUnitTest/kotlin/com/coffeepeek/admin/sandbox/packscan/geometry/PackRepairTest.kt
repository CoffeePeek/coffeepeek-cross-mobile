package com.coffeepeek.admin.sandbox.packscan.geometry

import kotlin.test.*

class PackRepairTest {
    @Test fun dilationGrowsASquareAroundEachPixel() {
        val grown = dilate(cells(".....", ".....", "..#..", ".....", "....."), width = 5, radius = 1)
        assertEquals(cells(".....", ".###.", ".###.", ".###.", ".....").toList(), grown.toList())
    }

    @Test fun outsidePixelsTakeNearestBagPixelOfTheirRowAndEmptyRowsCopyNearestRow() {
        val pixels = intArrayOf(0, 0, 0, 0, 1, 2, 0, 3, 0)
        extendOutward(pixels, cells("...", ".##", ".#."), width = 3)
        assertEquals(listOf(1, 1, 2, 1, 1, 2, 3, 3, 3), pixels.toList())
    }

    @Test fun maskWithoutBagLeavesPixelsUntouched() {
        val pixels = intArrayOf(7, 8, 9, 10)
        extendOutward(pixels, BooleanArray(4), width = 2)
        assertEquals(listOf(7, 8, 9, 10), pixels.toList())
    }

    private fun cells(vararg rows: String) = rows.joinToString("").map { it == '#' }.toBooleanArray()
}
