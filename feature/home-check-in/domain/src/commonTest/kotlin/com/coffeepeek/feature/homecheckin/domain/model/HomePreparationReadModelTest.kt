package com.coffeepeek.feature.homecheckin.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomePreparationReadModelTest {
    @Test fun rangesUseStrictCivilDatesAndAllowOneDayAcrossLeapAndYearBoundaries() {
        for (date in listOf("2000-02-29", "2024-02-29", "2026-10-16", "9999-12-31")) {
            assertEquals(date, HomePreparationDateRange(date, date).from)
        }
        assertEquals("2027-01-01", HomePreparationDateRange("2026-12-31", "2027-01-01").to)
        for (date in listOf("1900-02-29", "2026-02-29", "2026-04-31", "0000-01-01", "2026-1-01",
            "2026-10-16T00:00:00Z", "2026-00-01", "2026-13-01", "2026-01-00")) {
            assertFailsWith<IllegalArgumentException> { HomePreparationDateRange(date, date) }
        }
        assertFailsWith<IllegalArgumentException> { HomePreparationDateRange("2026-10-16", "2026-10-15") }
    }

    @Test fun pageMetadataUsesSafeMultiplicationAndRejectsInvalidQueries() {
        assertTrue(HomePreparationPage(emptyList(), Int.MAX_VALUE, 10000, 100).hasMore)
        assertFalse(HomePreparationPage(emptyList(), 100, 1, 100).hasMore)
        assertTrue(HomePreparationPage(emptyList(), 101, 1, 100).hasMore)
        for ((page, size, count) in listOf(Triple(0, 20, 0), Triple(10001, 20, 0), Triple(1, 0, 0),
            Triple(1, 101, 0), Triple(1, 20, -1))) {
            assertFailsWith<IllegalArgumentException> { HomePreparationPage(emptyList(), count, page, size) }
        }
    }

    @Test fun calendarCountsNeverInventDatesOrNegativePreparationCounts() {
        assertEquals(0, HomePreparationDay("2026-10-16", 0).brewCount)
        assertFailsWith<IllegalArgumentException> { HomePreparationDay("2026-02-30", 1) }
        assertFailsWith<IllegalArgumentException> { HomePreparationDay("2026-10-16", -1) }
    }
}
