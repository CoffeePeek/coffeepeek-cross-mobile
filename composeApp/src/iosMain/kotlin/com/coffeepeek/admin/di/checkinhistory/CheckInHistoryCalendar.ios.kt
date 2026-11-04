package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryDateBounds
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import kotlin.time.ExperimentalTime

internal actual fun deviceCheckInHistoryCalendar(): CheckInHistoryCalendar = CheckInHistoryCalendar { snapshot(NSTimeZone.localTimeZone) }

/** Captures the zone and today once; month bounds come from the zone's rules, not today's UTC offset. */
@OptIn(ExperimentalTime::class)
private fun snapshot(zone: NSTimeZone): CheckInHistoryCalendarSnapshot {
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply { timeZone = zone }
    val day = NSDateFormatter().apply {
        this.calendar = calendar
        timeZone = zone
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        dateFormat = "yyyy-MM-dd"
    }
    val instant = NSISO8601DateFormatter()
    val capturedToday = day.stringFromDate(NSDate())
    return object : CheckInHistoryCalendarSnapshot {
        override val today = capturedToday
        override val zoneId = zone.name

        override fun monthPeriod(year: Int, month: Int): CheckInHistoryDateBounds {
            require(year in 1..9999)
            return CheckInHistoryDateBounds(instant.stringFromDate(monthStart(calendar, year, month)),
                instant.stringFromDate(monthStart(calendar, if (month == 12) year + 1 else year, if (month == 12) 1 else month + 1)))
        }

        override fun localDate(instant: String): String? = parseServerInstant(instant)
            ?.let { day.stringFromDate(NSDate.dateWithTimeIntervalSince1970(it.toEpochMilliseconds() / 1000.0)) }
    }
}

private fun monthStart(calendar: NSCalendar, year: Int, month: Int): NSDate = checkNotNull(calendar.dateFromComponents(NSDateComponents().apply {
    this.year = year.toLong()
    this.month = month.toLong()
    this.day = 1
})) { "No such month: $year-$month" }
