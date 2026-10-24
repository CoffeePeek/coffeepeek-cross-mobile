package com.coffeepeek.feature.checkinhistory.impl.ui

import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.compareHistoryMoments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryTimelineOrderTest {
    private val day = "2026-10-16"
    private fun order(vararg stamps: String) = stamps.sortedWith { a, b -> compareHistoryMoments(day, a, day, b) }

    @Test fun anOffsetIsTakenIntoAccountNotJustTheText() {
        // 12:00+03:00 is 09:00Z, so the 10:00Z entry is newer although its text sorts lower.
        assertEquals(listOf("2026-10-16T10:00:00Z", "2026-10-16T12:00:00+03:00"), order("2026-10-16T12:00:00+03:00", "2026-10-16T10:00:00Z"))
    }

    @Test fun fractionsAndAMissingOffsetAreInstantsToo() {
        assertEquals(listOf("2026-10-16T10:00:00.5Z", "2026-10-16T10:00:00"), order("2026-10-16T10:00:00", "2026-10-16T10:00:00.5Z"))
    }

    @Test fun theCalendarDayComesFirstAndAnUnreadableStampGoesLast() {
        assertTrue(compareHistoryMoments("2026-10-17", "2026-10-17T01:00:00Z", "2026-10-16", "2026-10-16T23:00:00Z") < 0)
        assertEquals(listOf("2026-10-16T10:00:00Z", "garbage"), order("garbage", "2026-10-16T10:00:00Z"))
    }
}
