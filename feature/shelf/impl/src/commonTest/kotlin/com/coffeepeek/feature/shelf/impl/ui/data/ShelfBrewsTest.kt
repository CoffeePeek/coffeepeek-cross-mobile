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
    private val today = calendar.today()

    @Test fun aCustomBagMatchesItsNameButNotACatalogueCoffeeOfTheSameName() = runTest {
        val log = FakeShelfBrewLog(Result.success(listOf(
            brew("1", "2026-10-21", bean = "  bag A "),
            brew("2", "2026-10-22", product = "p9", bean = "Bag a"),
            brew("3", "2026-10-23", bean = "Other"),
        )))
        val brews = log.forBag(calendar.item(bag("a")), today) as ShelfBagBrews.Loaded
        assertEquals(listOf("1"), brews.brews.map { it.id })
    }

    @Test fun aFinishedBagLooksOnlyUntilTheDayItRanOut() = runTest {
        val log = FakeShelfBrewLog()
        log.forBag(calendar.item(bag("a", finished = true)), today)
        assertEquals(listOf("2026-10-20" to "2026-10-25"), log.ranges)
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
}
