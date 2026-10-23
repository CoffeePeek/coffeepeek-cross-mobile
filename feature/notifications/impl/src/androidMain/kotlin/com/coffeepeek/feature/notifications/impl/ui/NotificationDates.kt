package com.coffeepeek.feature.notifications.impl.ui

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private fun instant(value: String): Instant? = runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()

/** True when the instant falls on today's date in [zone]; an unreadable value counts as earlier. */
internal fun isToday(value: String, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
    instant(value)?.atZone(zone)?.toLocalDate() == today

/** The local date and time of the row, or the raw value when it cannot be read. */
internal fun localDateTime(value: String, zone: ZoneId = ZoneId.systemDefault()): String =
    instant(value)?.atZone(zone)?.format(DateTimeFormat) ?: value
