package com.coffeepeek.feature.checkinhistory.impl.ui.data

import kotlin.test.*

class ActivityDatesTest {
    @Test fun mondayAndSundayBelongToTheSameSevenCivilDays() {
        val monday = activityWeekDates("2026-10-19")
        assertEquals(monday, activityWeekDates("2026-10-25"))
        assertEquals(listOf("2026-10-19", "2026-10-20", "2026-10-21", "2026-10-22", "2026-10-23", "2026-10-24", "2026-10-25"), monday)
    }

    @Test fun leapDayAndYearRolloverDoNotInventDates() {
        assertEquals("2024-02-29", shiftActivityDate("2024-03-01", -1))
        assertEquals("2025-03-01", shiftActivityDate("2025-02-28", 1))
        assertEquals("2027-01-01", shiftActivityDate("2026-12-31", 1))
        assertEquals("2026-12-28", activityWeekDates("2027-01-01").first())
    }

    @Test fun invalidCivilDateIsRejected() {
        assertFailsWith<IllegalArgumentException> { activityWeekDates("2026-02-30") }
        assertFailsWith<IllegalArgumentException> { shiftActivityDate("2026-10-25", 8) }
    }
}
