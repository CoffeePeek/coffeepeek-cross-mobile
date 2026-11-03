package com.coffeepeek.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeePeekNativeSheetHeightTest {
    @Test
    fun expandedPanelStartsAtItsFinalHeightBeforeContentMeasurement() {
        val initial = coffeePeekNativeSheetDetentHeight(0.0, 800.0, 0.8f, true)
        assertEquals(640.0, initial, 0.001)
        assertEquals(initial, coffeePeekNativeSheetDetentHeight(240.0, 800.0, 0.8f, true))
        assertEquals(initial, coffeePeekNativeSheetDetentHeight(1_200.0, 800.0, 0.8f, true))
    }

    @Test
    fun expandedPanelStillFitsCompactWindows() {
        assertEquals(256.0, coffeePeekNativeSheetDetentHeight(900.0, 320.0, 0.8f, true), 0.001)
        assertTrue(coffeePeekNativeSheetDetentHeight(0.0, 0.0, 0.8f, true) > 0.0)
    }

    @Test
    fun existingSheetsKeepTheirProvisionalAndContentSizedDetents() {
        assertEquals(360.0, coffeePeekNativeSheetDetentHeight(0.0, 800.0, 0.9f), 0.001)
        assertEquals(240.0, coffeePeekNativeSheetDetentHeight(240.0, 800.0, 0.9f))
        assertEquals(720.0, coffeePeekNativeSheetDetentHeight(1_200.0, 800.0, 0.9f), 0.001)
    }
}
