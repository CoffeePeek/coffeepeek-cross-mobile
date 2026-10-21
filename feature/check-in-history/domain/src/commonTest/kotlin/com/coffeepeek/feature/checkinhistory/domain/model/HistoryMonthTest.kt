package com.coffeepeek.feature.checkinhistory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryMonthTest {
    @Test fun followsGregorianLeapYearRules() {
        assertEquals(29, HistoryMonth(2024, 2).daysInMonth)
        assertEquals(29, HistoryMonth(2000, 2).daysInMonth)
        assertEquals(28, HistoryMonth(1900, 2).daysInMonth)
        assertEquals(30, HistoryMonth(2026, 9).daysInMonth)
        assertEquals(31, HistoryMonth(2026, 10).daysInMonth)
    }

    @Test fun shiftsAcrossYearsInEitherDirection() {
        assertEquals(HistoryMonth(2027, 1), HistoryMonth(2026, 12).plusMonths(1))
        assertEquals(HistoryMonth(2025, 12), HistoryMonth(2026, 1).plusMonths(-1))
        assertEquals(HistoryMonth(2024, 12), HistoryMonth(2026, 1).plusMonths(-13))
        assertEquals(HistoryMonth(2026, 10), HistoryMonth(2026, 10).plusMonths(0))
    }

    @Test fun validatesCompleteDatesRatherThanJustTheirMonthPrefix() {
        val month = HistoryMonth(2026, 10)
        assertEquals("2026-10-01", month.date(1))
        assertEquals("0001-01-01", HistoryMonth(1, 1).date(1))
        assertTrue(month.containsDate("2026-10-31"))
        assertFalse(month.containsDate("2026-11-01"))
        for (value in listOf("2026-10-32", "2026-02-29", "0000-01-01", "2026-00-01", "2026-10",
            "2026-1-01", "2026-10-01T00:00:00Z", "2026-10-+1")) {
            assertNull(HistoryMonth.fromDate(value))
        }
        assertEquals(HistoryMonth(2024, 2), HistoryMonth.fromDate("2024-02-29"))
    }

    @Test fun rejectsInvalidOrOverflowingMonthsAndDays() {
        assertFailsWith<IllegalArgumentException> { HistoryMonth(2026, 0) }
        assertFailsWith<IllegalArgumentException> { HistoryMonth(0, 1) }
        assertFailsWith<IllegalArgumentException> { HistoryMonth(2026, 2).date(29) }
        assertFailsWith<IllegalArgumentException> { HistoryMonth(1, 1).plusMonths(-1) }
        assertFailsWith<IllegalArgumentException> { HistoryMonth(9999, 12).plusMonths(1) }
        assertFailsWith<IllegalArgumentException> { HistoryMonth(2026, 10).plusMonths(Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { HistoryMonth(2026, 10).plusMonths(Int.MIN_VALUE) }
    }
}
