package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileHistoryDatesTest {
    @Test fun collapsedWeekKeepsTheSelectedDateAndBrowsedMonthAnchor() {
        val month = HistoryMonth(2026, 9)
        val dates = profileHistoryDates("2026-10-16", month, "2026-09-30", false)
        assertTrue("2026-09-30" in dates)
        assertTrue("2026-10-01" in dates)
        assertEquals("2026-09-01", profileHistoryAnchor("2026-10-16", month, null))
        assertEquals("2026-10-16", profileHistoryAnchor("2026-10-16", HistoryMonth(2026, 10), null))
    }
}
