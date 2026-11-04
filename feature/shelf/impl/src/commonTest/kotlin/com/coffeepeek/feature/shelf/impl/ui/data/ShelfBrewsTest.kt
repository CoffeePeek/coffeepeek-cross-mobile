package com.coffeepeek.feature.shelf.impl.ui.data

import com.coffeepeek.feature.shelf.impl.ui.FakeShelfBrewLog
import com.coffeepeek.feature.shelf.impl.ui.bag
import com.coffeepeek.feature.shelf.impl.ui.brew
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.testCalendar
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShelfBrewsTest {
    private val calendar = testCalendar()

    @Test fun theBagAsksForItsOwnCheckInsNewestFirst() = runTest {
        val log = FakeShelfBrewLog(Result.success(listOf(brew("1", "2026-10-21"), brew("2", "2026-10-23"))))
        val brews = log.forBag(calendar.item(bag("a", finished = true))) as ShelfBagBrews.Loaded
        assertEquals(listOf("a"), log.bags)
        assertEquals(listOf("2", "1"), brews.brews.map { it.id })
    }

    @Test fun aFailedLoadIsReportedAsFailed() = runTest {
        val log = FakeShelfBrewLog(Result.failure(IllegalStateException("offline")))
        assertEquals(ShelfBagBrews.Failed, log.forBag(calendar.item(bag("a"))))
    }

    @Test fun whatIsLeftNeedsAWeightAndADoseAndNeverGoesBelowZero() {
        assertNull(shelfBagBrews(250, listOf(brew("1", "2026-10-21"))).leftGrams)
        assertNull(shelfBagBrews(null, listOf(brew("1", "2026-10-21", dose = 15.0))).leftGrams)
        assertEquals(0, shelfBagBrews(20, listOf(brew("1", "2026-10-21", dose = 15.0), brew("2", "2026-10-22", dose = 18.0))).leftGrams)
    }

    @Test fun outOfRangeScoresAndEmptyMethodsAreDropped() {
        val item = shelfBagBrews(250, listOf(brew("1", "2026-10-21", score = 9).copy(method = " "))).brews.single()
        assertNull(item.score)
        assertNull(item.method)
        assertEquals(ShelfDay(2026, 10, 21), item.day)
    }
    @Test fun genericOtherIsLocalizedAtRenderingAndContentNamesArePreserved() {
        val source = brew("1", "2026-10-21").copy(method = null, methodCode = "other")
        val item = shelfBagBrews(250, listOf(source)).brews.single()
        assertEquals("other", item.methodCode)
        assertEquals("Іншае", item.methodDisplayName("Іншае", "Не пазначана"))
        assertEquals("Другой авторский способ", item.copy(method = "Другой авторский способ").methodDisplayName("Іншае", "Не пазначана"))
        assertEquals("Не пазначана", item.copy(methodCode = null).methodDisplayName("Іншае", "Не пазначана"))
    }
}
