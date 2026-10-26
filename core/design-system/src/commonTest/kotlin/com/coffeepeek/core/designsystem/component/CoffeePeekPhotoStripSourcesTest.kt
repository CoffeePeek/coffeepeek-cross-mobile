package com.coffeepeek.core.designsystem.component

import kotlin.test.*

class CoffeePeekPhotoStripSourcesTest {
    @Test fun missingVariantsAndExplicitlyUnavailableVariantsRemainDifferent() {
        val absent = CoffeePeekPhotoStripSources(listOf("original"))
        val unavailable = CoffeePeekPhotoStripSources(listOf("original"), listOf(""), listOf(""))
        assertEquals("original", absent.display(0))
        assertEquals("", unavailable.display(0))
        assertNull(unavailable.upgrade(0))
        assertEquals(listOf(""), unavailable.previews)
        assertTrue(unavailable.canOpen(0))
    }

    @Test fun thumbnailUpgradesToDetailAndUnavailableThumbnailCanStartWithItsDetail() {
        val sources = CoffeePeekPhotoStripSources(listOf("o1", "o2"), listOf("t1", ""), listOf("d1", "d2"))
        assertEquals("t1", sources.display(0))
        assertEquals("d1", sources.upgrade(0))
        assertEquals("d2", sources.display(1))
        assertNull(sources.upgrade(1))
        assertEquals(listOf("t1", ""), sources.previews)
    }

    @Test fun sourceListsAreSnapshottedAndInvalidAlbumsCannotOpenTheViewer() {
        val originals = mutableListOf("o1", "o2")
        val previews = mutableListOf("t1")
        val sources = CoffeePeekPhotoStripSources(originals, previews)
        originals.clear()
        previews.clear()
        assertEquals(listOf("o1", "o2"), sources.originals)
        assertEquals(listOf("t1", "o2"), sources.previews)
        assertFalse(sources.canOpen(-1))
        assertFalse(sources.canOpen(2))
        assertFalse(CoffeePeekPhotoStripSources(listOf("o1", "")).canOpen(0))
    }
}
