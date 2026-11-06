package com.coffeepeek.feature.notifications.impl.ui

import com.coffeepeek.core.datetime.parseServerInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The server may leave the offset out: such a timestamp is UTC (the shared parser of core/datetime). */
@OptIn(ExperimentalTime::class)
private fun localDateOf(value: String, zone: TimeZone) = parseServerInstant(value)?.toLocalDateTime(zone)

@OptIn(ExperimentalTime::class)
private fun today(zone: TimeZone): LocalDate = Clock.System.now().toLocalDateTime(zone).date

/** True when the instant falls on today's date in [zone]; an unreadable value counts as earlier. */
internal fun isToday(value: String, today: LocalDate = today(TimeZone.currentSystemDefault()), zone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
    localDateOf(value, zone)?.date == today

/** The local date and time of the row as dd.MM.yyyy HH:mm, or the raw value when it cannot be read. */
internal fun localDateTime(value: String, zone: TimeZone = TimeZone.currentSystemDefault()): String =
    localDateOf(value, zone)?.let {
        "${it.dayOfMonth.pad()}.${it.monthNumber.pad()}.${it.year} ${it.hour.pad()}:${it.minute.pad()}"
    } ?: value

private fun Int.pad() = toString().padStart(2, '0')
