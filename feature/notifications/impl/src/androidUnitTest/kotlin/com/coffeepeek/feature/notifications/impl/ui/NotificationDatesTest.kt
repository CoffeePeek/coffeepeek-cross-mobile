package com.coffeepeek.feature.notifications.impl.ui

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationDatesTest {
    private val minsk = ZoneId.of("Europe/Minsk")

    @Test fun theLocalDateDecidesWhatIsToday() {
        // 23:30 UTC is already the next day in Minsk (UTC+3).
        assertTrue(isToday("2026-10-21T23:30:00Z", LocalDate.of(2026, 10, 22), minsk))
        assertFalse(isToday("2026-10-21T23:30:00Z", LocalDate.of(2026, 10, 21), minsk))
        assertFalse(isToday("not a date", LocalDate.of(2026, 10, 21), minsk))
    }

    @Test fun theRowShowsTheLocalDateAndTimeOrTheRawValueWhenItCannotBeRead() {
        assertEquals("21.10.2026 11:00", localDateTime("2026-10-21T08:00:00Z", minsk))
        assertEquals("yesterday", localDateTime("yesterday", minsk))
    }
}
