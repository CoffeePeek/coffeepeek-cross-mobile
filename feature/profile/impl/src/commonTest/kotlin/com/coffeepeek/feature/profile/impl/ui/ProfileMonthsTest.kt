package com.coffeepeek.feature.profile.impl.ui

import com.coffeepeek.feature.profile.impl.ui.data.ProfileMonth
import com.coffeepeek.feature.profile.impl.ui.data.profileMonth
import com.coffeepeek.feature.profile.impl.ui.data.shiftProfileMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileMonthsTest {
    @Test fun daysAndMondayFirstWeekdayAreRightForKnownMonths() {
        assertEquals(ProfileMonth(2026, 10, 31, 3), profileMonth("2026-10")) // 1 Oct 2026 is a Thursday
        assertEquals(ProfileMonth(2024, 2, 29, 3), profileMonth("2024-02")) // leap year, 1 Feb 2024 Thursday
        assertEquals(28, profileMonth("2100-02").days) // century is not a leap year
        assertEquals(29, profileMonth("2000-02").days)
        assertEquals(ProfileMonth(2025, 9, 30, 0), profileMonth("2025-09")) // 1 Sep 2025 is a Monday
    }

    @Test fun malformedMonthsDegradeInsteadOfCrashing() {
        assertEquals(ProfileMonth(2000, 1, 31, 5), profileMonth("garbage"))
        assertEquals(12, profileMonth("2026-99").number)
    }

    @Test fun shiftingStopsAtTheServersRange() {
        assertEquals("2026-11", shiftProfileMonth("2026-10", 1))
        assertEquals("2025-12", shiftProfileMonth("2026-01", -1))
        assertEquals("2027-01", shiftProfileMonth("2026-12", 1))
        assertNull(shiftProfileMonth("2000-01", -1))
        assertNull(shiftProfileMonth("2100-12", 1))
    }
}
