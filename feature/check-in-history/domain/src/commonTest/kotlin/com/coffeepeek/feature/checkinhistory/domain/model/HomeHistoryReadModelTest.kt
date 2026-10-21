package com.coffeepeek.feature.checkinhistory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeHistoryReadModelTest {
    @Test fun calendarBoundsAreFirstAndLastCivilDayNotShopExclusiveInstants() {
        assertEquals(HomeHistoryDateRange("2024-02-01", "2024-02-29"), HomeHistoryDateRange.month(HistoryMonth(2024, 2)))
        assertEquals(HomeHistoryDateRange("2026-12-01", "2026-12-31"), HomeHistoryDateRange.month(HistoryMonth(2026, 12)))
        assertEquals("2026-10-16", HomeHistoryDateRange("2026-10-16", "2026-10-16").to)
        for ((from, to) in listOf("2026-02-29" to "2026-03-01", "2026-10-17" to "2026-10-16",
            "2026-10-16T00:00:00Z" to "2026-11-01T00:00:00Z")) {
            assertFailsWith<IllegalArgumentException> { HomeHistoryDateRange(from, to) }
        }
    }

    @Test fun homePageAndDayCountsRejectInvalidMetadata() {
        assertTrue(HomeHistoryPage(emptyList(), Int.MAX_VALUE, 10000, 100).hasMore)
        assertFalse(HomeHistoryPage(emptyList(), 20, 1, 20).hasMore)
        assertFailsWith<IllegalArgumentException> { HomeHistoryPage(emptyList(), -1, 1, 20) }
        assertFailsWith<IllegalArgumentException> { HomeHistoryPage(emptyList(), 0, 0, 20) }
        assertFailsWith<IllegalArgumentException> { HomeHistoryPage(emptyList(), 0, 1, 101) }
        assertFailsWith<IllegalArgumentException> { HomeHistoryDay("2026-04-31", 1) }
        assertFailsWith<IllegalArgumentException> { HomeHistoryDay("2026-10-16", -1) }
        assertEquals(2, HomeHistoryDay("2026-10-16", 2).brewCount)
    }
}
