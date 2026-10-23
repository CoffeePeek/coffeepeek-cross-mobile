package com.coffeepeek.feature.discovery.impl.tab

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChoreographyTest {
    @Test fun thePreviewFollowsTheItemOffsetAndStaysAboveTheViewportWhenItsItemIsRecycled() {
        val anchor = Frame(16f, 180f, 384f, 420f)
        assertEquals(anchor, previewFrame(anchor, 164f, 16))
        // The offset follows the scroll; a pull to refresh moves the list but not the item offset, so the map is pinned (the measured anchor top is ignored).
        assertEquals(Frame(16f, 64f, 384f, 304f), previewFrame(anchor, 164f, -100))
        assertEquals(anchor, previewFrame(Frame(16f, 252f, 384f, 492f), 164f, 16))
        assertEquals(164f, previewFrame(anchor, 164f, null).bottom)
        assertEquals(anchor.width, previewFrame(anchor, 164f, null).width)
    }

    @Test fun theShopListPushesTheMapWithoutOverlappingItAndCancelReversesTheMotion() {
        val height = 800f
        for (progress in listOf(1f, 0.75f, 0.5f, 0.25f, 0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val offset = discoveryMapOffsetY(height, progress)
            assertEquals(height * progress, height + offset)
            assertTrue(240f + offset <= height * progress)
        }
        assertEquals(-height, discoveryMapOffsetY(height, -1f))
        assertEquals(0f, discoveryMapOffsetY(height, 2f))
    }

    @Test fun theMapExpandsFromItsPreviewToTheWholeScreen() {
        val preview = Frame(16f, 180f, 384f, 420f)
        assertEquals(preview, expandedFrame(preview, 400f, 800f, 0f))
        assertEquals(Frame(8f, 90f, 392f, 610f), expandedFrame(preview, 400f, 800f, 0.5f))
        assertEquals(Frame(0f, 0f, 400f, 800f), expandedFrame(preview, 400f, 800f, 1f))
        assertEquals(Frame(0f, 0f, 400f, 800f), expandedFrame(preview, 400f, 800f, 3f))
    }

    @Test fun theClipTopFollowsTheExpansionAndStaysOnTheScreen() {
        assertEquals(164, mapClipTop(164f, 0f, 800))
        assertEquals(82, mapClipTop(164f, 0.5f, 800))
        assertEquals(0, mapClipTop(164f, 1f, 800))
        assertEquals(800, mapClipTop(5000f, 0f, 800))
        assertEquals(0, mapClipTop(-10f, 0f, 800))
    }

    @Test fun theCornersFlattenWhileTheMapExpands() {
        assertEquals(24f, mapCornerRadius(24f, 0f)); assertEquals(12f, mapCornerRadius(24f, 0.5f)); assertEquals(0f, mapCornerRadius(24f, 1f)); assertEquals(0f, mapCornerRadius(24f, 2f))
    }
}
