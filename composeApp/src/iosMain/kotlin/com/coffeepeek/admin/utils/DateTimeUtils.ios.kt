@file:OptIn(kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.utils

import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarMatchFirst
import platform.Foundation.NSCalendarMatchNextTimePreservingSmallerUnits
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT

internal actual fun currentUtcIsoDateTime(): String = isoFormatter().stringFromDate(NSDate())

internal actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1_000.0).toLong()

internal actual fun currentLocalDayOfWeek(): Int =
    dateFormatter("e").stringFromDate(NSDate()).toIntOrNull()?.minus(1) ?: 0

internal actual fun currentLocalMinuteOfDay(): Int {
    val now = NSDate()
    return dateFormatter("HH").stringFromDate(now).toInt() * 60 + dateFormatter("mm").stringFromDate(now).toInt()
}

internal actual fun datePickerMillisToUtcIsoInstant(millis: Long): String {
    val selectedUtcDate = dateFromEpochMillis(millis)
    val date = dateFormatter("yyyy-MM-dd", utc = true).stringFromDate(selectedUtcDate)
    val localMidnight = dateFormatter("yyyy-MM-dd").dateFromString(date) ?: selectedUtcDate
    return isoFormatter().stringFromDate(localMidnight)
}

internal actual fun instantToDatePickerMillis(millis: Long): Long {
    val localDate = dateFormatter("yyyy-MM-dd").stringFromDate(dateFromEpochMillis(millis))
    val pickerDate = checkNotNull(dateFormatter("yyyy-MM-dd", utc = true).dateFromString(localDate))
    return (pickerDate.timeIntervalSince1970 * 1000).toLong()
}

internal actual fun datePickerMillisWithLocalTime(dateMillis: Long, referenceMillis: Long): Long {
    val reference = dateFromEpochMillis(referenceMillis)
    val selectedDate = dateFormatter("yyyy-MM-dd", utc = true).stringFromDate(dateFromEpochMillis(dateMillis))
    // Some time zones advance their clocks at midnight; that day starts at 01:00.
    val localDates = dateFormatter("yyyy-MM-dd").apply { lenient = true }
    if (selectedDate == localDates.stringFromDate(reference)) return referenceMillis
    val midnight = checkNotNull(localDates.dateFromString(selectedDate))
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply {
        timeZone = NSTimeZone.localTimeZone()
    }
    val clock = calendar.components(NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond, reference)
    val visit = checkNotNull(calendar.dateBySettingHour(
        clock.hour, minute = clock.minute, second = clock.second, ofDate = midnight,
        options = NSCalendarMatchNextTimePreservingSmallerUnits or NSCalendarMatchFirst,
    ))
    return (visit.timeIntervalSince1970 * 1000).toLong() + referenceMillis.mod(1000L)
}

internal actual fun utcIsoToLocalDate(value: String): String =
    parseUtcDate(value)?.let { dateFormatter("yyyy-MM-dd").stringFromDate(it) }
        ?: value.substringBefore('T').ifBlank { value }

internal actual fun utcIsoToLocalDateTime(value: String): String =
    parseUtcDate(value)?.let { dateFormatter("dd.MM.yyyy HH:mm").stringFromDate(it) } ?: value

internal actual fun formatVisitDate(millis: Long): String = NSDateFormatter().run {
    locale = NSLocale(localeIdentifier = "ru_RU")
    dateFormat = "d MMMM yyyy"
    timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    stringFromDate(dateFromEpochMillis(millis))
}

private fun isoFormatter(fractionalSeconds: Boolean = false) = NSISO8601DateFormatter().apply {
    if (fractionalSeconds) formatOptions = formatOptions or NSISO8601DateFormatWithFractionalSeconds
}

private fun parseUtcDate(value: String): NSDate? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val formatter = isoFormatter(fractionalSeconds = '.' in trimmed.substringAfter('T', ""))
    formatter.dateFromString(trimmed)?.let { return it }
    return formatter.dateFromString("${trimmed}Z")
}

private fun dateFormatter(pattern: String, utc: Boolean = false) = NSDateFormatter().apply {
    locale = NSLocale(localeIdentifier = "en_US_POSIX")
    dateFormat = pattern
    timeZone = if (utc) NSTimeZone.timeZoneForSecondsFromGMT(0) else NSTimeZone.localTimeZone()
}

private fun dateFromEpochMillis(millis: Long): NSDate =
    NSDate(timeIntervalSinceReferenceDate = millis / 1_000.0 - UNIX_REFERENCE_DATE_OFFSET_SECONDS)

private const val UNIX_REFERENCE_DATE_OFFSET_SECONDS = 978_307_200.0
