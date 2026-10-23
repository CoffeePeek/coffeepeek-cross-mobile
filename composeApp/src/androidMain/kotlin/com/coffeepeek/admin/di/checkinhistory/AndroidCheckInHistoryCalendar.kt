package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryDateBounds
import com.coffeepeek.core.datetime.parseServerJavaInstant
import java.time.Clock
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

            override fun localDate(instant: String): String? = parseServerJavaInstant(instant)
                ?.atZone(capturedZone)?.toLocalDate()?.toString()
        }
    }
}
