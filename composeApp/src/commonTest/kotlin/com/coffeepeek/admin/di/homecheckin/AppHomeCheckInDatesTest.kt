package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.utils.utcIsoToLocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AppHomeCheckInDatesTest {
    @Test fun leapDayUsesTheUtcMidnightPickerToken() {
        assertEquals(Instant.parse("2024-02-29T00:00:00Z").toEpochMilliseconds(), civilDateMillis("2024-02-29"))
    }

    @Test fun invalidCivilDatesHaveNoInventedReplacementDay() {
        listOf("", "not-a-date", "2023-02-29", "2026-13-01").forEach { assertNull(civilDateMillis(it)) }
    }

    @Test fun unreadableServerDateIsPreservedForDisplay() {
        assertEquals("not-a-date", appHomeCheckInDates().formatDate("not-a-date"))
    }

    @Test fun selectedDateRoundTripsThroughThePlatformMidnightConversion() {
        val instant = appHomeCheckInDates().civilDateToInstant("2026-10-14")
        assertEquals("2026-10-14", utcIsoToLocalDate(instant))
    }
}
