package com.coffeepeek.admin.utils

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeUtilsTest {
    @Test
    fun visitInstantKeepsHoursMinutesSecondsAndMillisecondsWhenSerialized() {
        val value = "2026-10-14T20:37:42.123Z"
        assertEquals(value, epochMillisToUtcIsoInstant(Instant.parse(value).toEpochMilli()))
    }

    @Test
    fun calendarUsesTheLocalDayOnBothSidesOfUtcMidnight() {
        val instant = Instant.parse("2026-10-14T22:30:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-10-15T00:00:00Z").toEpochMilli(),
            instantToDatePickerMillis(instant, ZoneId.of("Europe/Minsk")))
        assertEquals(Instant.parse("2026-10-14T00:00:00Z").toEpochMilli(),
            instantToDatePickerMillis(instant, ZoneId.of("America/Los_Angeles")))
    }

    @Test
    fun changingVisitDayRetainsLocalTimeAndFractionalSeconds() {
        val selected = Instant.parse("2026-10-12T00:00:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-10-12T20:37:42.123Z").toEpochMilli(),
            datePickerMillisWithLocalTime(selected, Instant.parse("2026-10-14T20:37:42.123Z").toEpochMilli(), ZoneId.of("Europe/Minsk")))
        assertEquals(Instant.parse("2026-10-13T03:37:42.123Z").toEpochMilli(),
            datePickerMillisWithLocalTime(selected, Instant.parse("2026-10-14T03:37:42.123Z").toEpochMilli(), ZoneId.of("America/Los_Angeles")))
    }

    @Test
    fun changingDayUsesTheSelectedDaysOffsetAcrossDaylightSavingTime() {
        val ny = ZoneId.of("America/New_York")
        assertEquals(Instant.parse("2026-03-08T13:37:42.123Z").toEpochMilli(), datePickerMillisWithLocalTime(
            Instant.parse("2026-03-08T00:00:00Z").toEpochMilli(),
            Instant.parse("2026-03-07T14:37:42.123Z").toEpochMilli(), ny))
        // The missing 02:37 local time moves forward to 03:37 rather than dropping to midnight.
        assertEquals(Instant.parse("2026-03-08T07:37:42.123Z").toEpochMilli(), datePickerMillisWithLocalTime(
            Instant.parse("2026-03-08T00:00:00Z").toEpochMilli(),
            Instant.parse("2026-03-07T07:37:42.123Z").toEpochMilli(), ny))
        assertEquals(Instant.parse("2018-11-04T03:37:42.123Z").toEpochMilli(), datePickerMillisWithLocalTime(
            Instant.parse("2018-11-04T00:00:00Z").toEpochMilli(),
            Instant.parse("2018-11-03T03:37:42.123Z").toEpochMilli(), ZoneId.of("America/Sao_Paulo")))
    }

    @Test
    fun confirmingTheSameDayKeepsTheExactInstantDuringARepeatedDstHour() {
        val repeatedHour = Instant.parse("2026-11-01T06:37:42.123Z").toEpochMilli()
        assertEquals(repeatedHour, datePickerMillisWithLocalTime(
            Instant.parse("2026-11-01T00:00:00Z").toEpochMilli(), repeatedHour, ZoneId.of("America/New_York")))
    }

    @Test
    fun localCalendarDateIsSentAsUtcAndBackendUtcIsShownLocally() {
        val kyiv = ZoneId.of("Europe/Kyiv")

        assertEquals(
            "2026-06-14T21:00:00Z",
            datePickerMillisToUtcIsoInstant(
                Instant.parse("2026-06-15T00:00:00Z").toEpochMilli(),
                kyiv,
            ),
        )
        assertEquals("2026-06-16", utcIsoToLocalDate("2026-06-15T22:30:00Z", kyiv))
        assertEquals(
            "16.06.2026 01:30",
            utcIsoToLocalDateTime("2026-06-15T22:30:00Z", kyiv),
        )
    }
}
