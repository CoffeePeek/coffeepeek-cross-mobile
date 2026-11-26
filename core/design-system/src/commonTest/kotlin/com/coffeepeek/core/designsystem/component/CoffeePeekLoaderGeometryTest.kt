package com.coffeepeek.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeePeekLoaderGeometryTest {
    @Test fun bothRenderersReceiveFiniteNormalizedContoursWithMatchingPointCounts() {
        val contours = CoffeePeekLoaderGeometry.contours
        assertEquals(5, contours.size)
        contours.forEach { contour ->
            assertEquals(64, contour.size)
            contour.forEach { point ->
                assertTrue(point.x.isFinite() && point.y.isFinite())
                val radiusSquared = point.x * point.x + point.y * point.y
                assertTrue(radiusSquared > 0f && radiusSquared <= 1f)
            }
        }
    }
}
