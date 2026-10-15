package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryDateBounds
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Capture zone and today once per read; month bounds use zone rules, not today's UTC offset. */
internal class AndroidCheckInHistoryCalendar(
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun snapshot(): CheckInHistoryCalendarSnapshot {
        val capturedZone = zone()
        val capturedToday = Instant.now(clock).atZone(capturedZone).toLocalDate().toString()
        return object : CheckInHistoryCalendarSnapshot {
            override val today = capturedToday
            override val zoneId = capturedZone.id

            override fun monthPeriod(year: Int, month: Int): CheckInHistoryDateBounds {
                require(year in 1..9999)
                val first = YearMonth.of(year, month).atDay(1)
                return CheckInHistoryDateBounds(
                    first.atStartOfDay(capturedZone).toInstant().toString(),
                    first.plusMonths(1).atStartOfDay(capturedZone).toInstant().toString(),
                )
            }

            override fun localDate(instant: String): String? = parseUtcInstant(instant)
                ?.atZone(capturedZone)?.toLocalDate()?.toString()
        }
    }

    /** Preserve the application's existing UTC fallback for server timestamps without a zone suffix. */
    private fun parseUtcInstant(value: String): Instant? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return try { Instant.parse(trimmed) } catch (_: DateTimeException) {
            try { Instant.parse("${trimmed}Z") } catch (_: DateTimeException) { null }
        }
    }
}
