package com.coffeepeek.admin.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeSheetDetentHeightTest {
    @Test
    fun emptyOrErrorContentCanPresentBeforeMeasurement() {
        assertEquals(360.0, nativeSheetDetentHeight(0.0, 800.0, 0.9f), 0.001)
        assertTrue(nativeSheetDetentHeight(0.0, 0.0, 1f) > 0.0)
        assertEquals(24.0, nativeSheetDetentHeight(24.0, 800.0, 0.9f))
    }

    @Test
    fun measuredHeightReplacesTheProvisionalHeightWithinTheCurrentViewport() {
        assertEquals(540.0, nativeSheetDetentHeight(540.0, 800.0, 0.9f))
        assertEquals(720.0, nativeSheetDetentHeight(900.0, 800.0, 0.9f), 0.001)
        assertEquals(360.0, nativeSheetDetentHeight(540.0, 400.0, 0.9f), 0.001)
    }
}
