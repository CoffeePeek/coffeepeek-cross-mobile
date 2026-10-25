package com.coffeepeek.admin.di.checkinhistory

import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AppCheckInHistoryCalendarTest {
    private val clock: () -> Instant = { Instant.parse("2026-10-14T22:30:00Z") }

    @Test fun minskBoundsAndGroupingUseTheSameLocalMonth() {
        val snapshot = AppCheckInHistoryCalendar({ TimeZone.of("Europe/Minsk") }, clock).snapshot()
        assertEquals("2026-10-15", snapshot.today)
        assertEquals("Europe/Minsk", snapshot.zoneId)
        val bounds = snapshot.monthPeriod(2026, 10)
        assertEquals("2026-09-30T21:00:00Z", bounds.fromInclusive)
        assertEquals("2026-10-31T21:00:00Z", bounds.toExclusive)
        assertEquals("2026-09-30", snapshot.localDate("2026-09-30T20:59:59Z"))
        assertEquals("2026-10-01", snapshot.localDate(bounds.fromInclusive))
        assertEquals("2026-11-01", snapshot.localDate(bounds.toExclusive))
    }

    @Test fun dstMonthsUseDifferentOffsetsAtTheirTwoBoundaries() {
        val snapshot = AppCheckInHistoryCalendar({ TimeZone.of("America/New_York") }, clock).snapshot()
        val spring = snapshot.monthPeriod(2026, 3)
        assertEquals("2026-03-01T05:00:00Z", spring.fromInclusive)
        assertEquals("2026-04-01T04:00:00Z", spring.toExclusive)
        val autumn = snapshot.monthPeriod(2026, 11)
        assertEquals("2026-11-01T04:00:00Z", autumn.fromInclusive)
        assertEquals("2026-12-01T05:00:00Z", autumn.toExclusive)
    }

    @Test fun midnightTransitionUsesTheFirstValidLocalTime() {
        val snapshot = AppCheckInHistoryCalendar({ TimeZone.of("America/Havana") }, clock).snapshot()
        assertEquals("2012-04-01T05:00:00Z", snapshot.monthPeriod(2012, 4).fromInclusive)
        assertEquals("2012-04-01", snapshot.localDate("2012-04-01T05:00:00Z"))
    }

    @Test fun snapshotDoesNotMixAnOldZoneWithNewGrouping() {
        var zone = TimeZone.of("Europe/Minsk")
        val calendar = AppCheckInHistoryCalendar({ zone }, clock)
        val first = calendar.snapshot()
        zone = TimeZone.of("America/New_York")
        val second = calendar.snapshot()
        assertEquals("2026-10-15", first.today)
        assertEquals("2026-10-14", second.today)
        assertEquals("2026-10-15", first.localDate("2026-10-14T22:30:00Z"))
        assertEquals("2026-10-14", second.localDate("2026-10-14T22:30:00Z"))
        assertEquals("2026-09-30T21:00:00Z", first.monthPeriod(2026, 10).fromInclusive)
        assertEquals("2026-10-01T04:00:00Z", second.monthPeriod(2026, 10).fromInclusive)
    }

    @Test fun newSnapshotsFollowTheClockAcrossLocalMidnight() {
        var now = Instant.parse("2026-10-14T20:59:59Z")
        val moving: () -> Instant = { now }
        val calendar = AppCheckInHistoryCalendar({ TimeZone.of("Europe/Minsk") }, moving)
        assertEquals("2026-10-14", calendar.snapshot().today)
        now += 1.seconds
        assertEquals("2026-10-15", calendar.snapshot().today)
    }

    @Test fun yearLeapAndOffsetTimestampInputsAreSupportedWithoutFakeDates() {
        val snapshot = AppCheckInHistoryCalendar({ TimeZone.UTC }, clock).snapshot()
        assertEquals("2025-01-01T00:00:00Z", snapshot.monthPeriod(2024, 12).toExclusive)
        assertEquals("2024-03-01T00:00:00Z", snapshot.monthPeriod(2024, 2).toExclusive)
        assertEquals("2026-09-30", snapshot.localDate("2026-10-01T01:00:00+03:00"))
        assertEquals("2026-10-01", snapshot.localDate(" 2026-10-01T01:00:00 "))
        assertNull(snapshot.localDate(" "))
        assertNull(snapshot.localDate("not-a-date"))
        assertFailsWith<IllegalArgumentException> { snapshot.monthPeriod(0, 1) }
        assertFailsWith<IllegalArgumentException> { snapshot.monthPeriod(2026, 13) }
    }
}
