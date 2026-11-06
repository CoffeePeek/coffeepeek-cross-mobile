package com.coffeepeek.admin.di.contributions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class ContributionTimeZoneTest {
    @Test fun signedAndFractionalOffsetsArePreserved() {
        val instant = Instant.parse("2026-10-25T12:00:00Z")
        assertEquals(180, contributionUtcOffsetMinutes(instant, TimeZone.of("UTC+03:00")))
        assertEquals(-210, contributionUtcOffsetMinutes(instant, TimeZone.of("UTC-03:30")))
        assertEquals(345, contributionUtcOffsetMinutes(instant, TimeZone.of("UTC+05:45")))
    }

    @Test fun daylightSavingIsResolvedForTheSubmissionInstant() {
        val zone = TimeZone.of("Europe/Berlin")
        assertEquals(60, contributionUtcOffsetMinutes(Instant.parse("2026-01-15T12:00:00Z"), zone))
        assertEquals(120, contributionUtcOffsetMinutes(Instant.parse("2026-07-15T12:00:00Z"), zone))
    }
}
