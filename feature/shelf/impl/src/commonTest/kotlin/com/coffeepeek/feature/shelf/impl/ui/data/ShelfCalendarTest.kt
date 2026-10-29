package com.coffeepeek.feature.shelf.impl.ui.data

import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import com.coffeepeek.feature.shelf.impl.ui.bag
import com.coffeepeek.feature.shelf.impl.ui.testCalendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShelfCalendarTest {
    @Test fun epochDaysMatchKnownDatesBothWays() {
        mapOf(
            ShelfDay(1970, 1, 1) to 0L, ShelfDay(2000, 2, 29) to 11_016L, ShelfDay(2026, 10, 27) to 20_753L,
            ShelfDay(1969, 12, 31) to -1L, ShelfDay(2100, 3, 1) to 47_541L,
        ).forEach { (day, epochDay) ->
            assertEquals(epochDay, day.epochDay, "$day")
            assertEquals(day, ShelfDay.ofEpochDay(epochDay))
        }
    }

    @Test fun parsesOnlyRealIsoDates() {
        assertEquals(ShelfDay(2026, 10, 6), ShelfDay.parse("2026-10-06"))
        assertNull(ShelfDay.parse("2026-02-30"))
        assertNull(ShelfDay.parse("2026-13-01"))
        assertNull(ShelfDay.parse("06.10.2026"))
        assertNull(ShelfDay.parse(null))
    }

    @Test fun serverInstantsFollowTheUserTimeZone() {
        val minsk = ShelfTimeZone { 3 * 3600 }
        val calendar = testCalendar(now = "2026-10-31T22:30:00Z", zone = minsk)
        assertEquals(ShelfDay(2026, 11, 1), calendar.today())
        val item = calendar.item(bag("a", addedAt = "2026-10-20T21:30:00Z"))
        assertEquals(ShelfDay(2026, 10, 21), item.addedOn)
        assertEquals(11, item.daysOnShelf(calendar.today()))
    }

    @Test fun finishedBagCountsDaysUntilItsFinishAndUnreadableDatesAreUnknown() {
        val calendar = testCalendar()
        val finished = calendar.item(bag("a", finished = true, addedAt = "2026-10-01T10:00:00Z", finishedAt = "2026-10-24T10:00:00"))
        assertEquals(23, finished.daysOnShelf(calendar.today()))
        assertNull(calendar.item(bag("b", addedAt = "yesterday")).daysOnShelf(calendar.today()))
    }
}
