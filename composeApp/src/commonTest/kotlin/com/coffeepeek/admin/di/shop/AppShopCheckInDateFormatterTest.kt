package com.coffeepeek.admin.di.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AppShopCheckInDateFormatterTest {
    private val now = Instant.parse("2026-10-25T12:00:00Z").toEpochMilliseconds()
    private val dates = AppShopCheckInDateFormatter { now }

    @Test fun invalidTimestampHasNoPickerDateOrLabel() {
        assertNull(dates.pickerMillis("invalid"))
        assertEquals("", dates.label("invalid"))
    }

    @Test fun equivalentOffsetsSelectTheSameLocalDay() {
        assertEquals(dates.pickerMillis("2026-10-24T23:30:00Z"), dates.pickerMillis("2026-10-25T02:30:00+03:00"))
    }

    @Test fun retainingTheDayKeepsTheExactVisitInstant() {
        val reference = "2026-10-01T12:34:56.789Z"
        val day = requireNotNull(dates.pickerMillis(reference))
        assertEquals(Instant.parse(reference), Instant.parse(dates.visitInstant(day, reference)))
    }

    @Test fun aFutureDayIsClampedToTheCurrentInstant() {
        val reference = "2026-10-01T12:34:56.789Z"
        val futureDay = requireNotNull(dates.pickerMillis("2050-10-01T12:00:00Z"))
        assertEquals(now, Instant.parse(dates.visitInstant(futureDay, reference)).toEpochMilliseconds())
    }
}
