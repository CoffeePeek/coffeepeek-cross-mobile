package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryDateBounds
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Capture zone and today together; boundaries use the rules at each local date, including DST. */
internal class AppCheckInHistoryCalendar(
    private val zone: () -> TimeZone = TimeZone::currentSystemDefault,
    private val now: () -> Instant = { Clock.System.now() },
) {
    fun snapshot(): CheckInHistoryCalendarSnapshot {
        val captured = zone()
        return CapturedHistoryCalendar(captured, now().toLocalDateTime(captured).date.toString())
    }
}

private class CapturedHistoryCalendar(private val zone: TimeZone, override val today: String) : CheckInHistoryCalendarSnapshot {
    override val zoneId = zone.id

    override fun monthPeriod(year: Int, month: Int): CheckInHistoryDateBounds {
        require(year in 1..9999)
        val first = LocalDate(year, month, 1)
        return datePeriod(first.toString(), first.plus(1, DateTimeUnit.MONTH).toString())
    }

    override fun datePeriod(fromDate: String, toExclusiveDate: String): CheckInHistoryDateBounds {
        val from = LocalDate.parse(fromDate)
        val to = LocalDate.parse(toExclusiveDate)
        require(from < to)
        return CheckInHistoryDateBounds(from.atStartOfDayIn(zone).toString(), to.atStartOfDayIn(zone).toString())
    }

    override fun localDate(instant: String): String? =
        parseServerInstant(instant)?.toLocalDateTime(zone)?.date?.toString()
}
