package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileActivityDatesTest {
    @Test fun expandedMonthHasOnlyItsActualWeeks() {
        val february = profileActivityDates("2026-11-17", HistoryMonth(2021, 2), true)
        val october = profileActivityDates("2026-11-17", HistoryMonth(2026, 10), true)
        assertEquals(28, february.size)
        assertEquals("2021-02-01", february.first())
        assertEquals("2021-02-28", february.last())
        assertEquals(35, october.size)
        assertEquals(31, october.filterNotNull().size)
        assertEquals("2026-10-31", october[33])
        assertNull(october.last())
    }

    @Test fun collapsedWeekCrossesYearWithoutUsingBrowsedMonth() {
        val dates = profileActivityDates("2027-01-01", HistoryMonth(2026, 10), false)
        assertEquals(listOf("2026-12-28", "2026-12-29", "2026-12-30", "2026-12-31",
            "2027-01-01", "2027-01-02", "2027-01-03"), dates)
    }

    @Test fun expandedMonthKeepsMondayFirstBlanksAndLeapDay() {
        val dates = profileActivityDates("2028-02-17", HistoryMonth(2028, 2), true)
        assertEquals(35, dates.size)
        assertNull(dates.first())
        assertEquals("2028-02-01", dates[1])
        assertEquals("2028-02-29", dates[29])
        assertTrue(dates.drop(30).all { it == null })
    }

    @Test fun referenceNovemberHasSixRowsAndCollapsedCurrentWeek() {
        val month = HistoryMonth(2026, 11)
        val expanded = profileActivityDates("2026-11-17", month, true)
        assertEquals(42, expanded.size)
        assertEquals("2026-11-01", expanded[6])
        assertEquals("2026-11-30", expanded[35])
        assertEquals((16..22).map { "2026-11-$it" }, profileActivityDates("2026-11-17", month, false))
    }
}
