package com.coffeepeek.admin.sandbox.packscan.geometry

import kotlin.test.*

class PackMaskGeometryTest {
    @Test fun hullOfRectangleIsItsOuterCorners() {
        val hull = mask(".....", ".###.", ".###.", ".....").convexHull()
        assertEquals(setOf(PackPoint(1f, 1f), PackPoint(4f, 1f), PackPoint(4f, 3f), PackPoint(1f, 3f)), hull.toSet())
    }

    @Test fun hullRestoresEdgeBittenOutByGlare() {
        val bitten = mask("####", "##..", "##..", "####")
        assertEquals(12, bitten.area)
        assertEquals(setOf(PackPoint(0f, 0f), PackPoint(4f, 0f), PackPoint(4f, 4f), PackPoint(0f, 4f)), bitten.convexHull().toSet())
    }

    @Test fun hullIgnoresLowConfidenceAndEmptyMasks() {
        assertTrue(PackMask(2, 2, floatArrayOf(0.5f, 0.1f, 0f, 0.4f)).convexHull().isEmpty())
        assertEquals(0, PackMask(2, 2, FloatArray(4)).area)
    }

    @Test fun maskRejectsMismatchedSize() {
        assertFailsWith<IllegalArgumentException> { PackMask(3, 3, FloatArray(4)) }
    }

    private fun mask(vararg rows: String) = PackMask(
        rows.first().length,
        rows.size,
        FloatArray(rows.sumOf { it.length }) { i -> if (rows[i / rows.first().length][i % rows.first().length] == '#') 1f else 0f },
    )
}
