package com.coffeepeek.core.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoPrefetchTest {
    @Test fun usesVisibleKeysDespiteHeadersAndStopsAtTheLookaheadBudget() {
        val photos = List(20) { index -> "post-$index" to List(3) { "photo-$index-$it" } }
        assertEquals(photos.drop(5).take(2).flatMap { it.second },
            upcomingPhotoUrls(photos, listOf("heading", "post-3", "post-4"), atStart = false))
    }

    @Test fun filtersEmptyAndDuplicateUrlsWithoutGoingBeyondThreeCards() {
        val photos = listOf("1" to listOf("", "same"), "2" to listOf("same", "second"),
            "3" to emptyList(), "4" to listOf("too-far"))
        assertEquals(listOf("same", "second"), upcomingPhotoUrls(photos, listOf("heading"), atStart = true))
        assertTrue(upcomingPhotoUrls(photos, listOf("footer"), atStart = false).isEmpty())
        assertTrue(upcomingPhotoUrls(photos, listOf("4"), atStart = false).isEmpty())
    }

    @Test fun refreshAndScrollDirectionUseTheCurrentItemOrder() {
        val photos = listOf("c" to listOf("C"), "a" to listOf("A"), "b" to listOf("B"))
        assertEquals(listOf("A", "B"), upcomingPhotoUrls(photos, listOf("c"), atStart = true))
        assertEquals(listOf("B"), upcomingPhotoUrls(photos, listOf("a"), atStart = false))
    }
}
