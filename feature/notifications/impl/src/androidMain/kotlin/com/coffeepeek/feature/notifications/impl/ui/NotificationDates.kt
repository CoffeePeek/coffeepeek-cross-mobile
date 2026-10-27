package com.coffeepeek.feature.notifications.impl.ui

import com.coffeepeek.core.datetime.parseServerJavaInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/** The server may leave the offset out: such a timestamp is UTC (the shared parser of core/datetime). */
private fun instant(value: String): Instant? = parseServerJavaInstant(value)

/** True when the instant falls on today's date in [zone]; an unreadable value counts as earlier. */
internal fun isToday(value: String, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
    instant(value)?.atZone(zone)?.toLocalDate() == today

/** The local date and time of the row, or the raw value when it cannot be read. */
internal fun localDateTime(value: String, zone: ZoneId = ZoneId.systemDefault()): String =
    instant(value)?.atZone(zone)?.format(DateTimeFormat) ?: value
