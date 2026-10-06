package com.coffeepeek.admin.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal actual fun currentUtcIsoDateTime(): String = Instant.now().toString()

internal actual fun currentEpochMillis(): Long = System.currentTimeMillis()

internal actual fun currentLocalDayOfWeek(): Int = LocalDate.now().dayOfWeek.value % 7

internal actual fun currentLocalMinuteOfDay(): Int = java.time.LocalTime.now().let { it.hour * 60 + it.minute }

internal actual fun datePickerMillisToUtcIsoInstant(millis: Long): String =
    datePickerMillisToUtcIsoInstant(millis, ZoneId.systemDefault())

internal actual fun utcIsoToLocalDate(value: String): String =
    utcIsoToLocalDate(value, ZoneId.systemDefault())

internal actual fun utcIsoToLocalDateTime(value: String): String =
    utcIsoToLocalDateTime(value, ZoneId.systemDefault())

internal fun utcIsoToLocalDateTime(value: String, zoneId: ZoneId): String =
    parseUtcInstant(value)
        ?.atZone(zoneId)
        ?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        ?: value

internal fun datePickerMillisToUtcIsoInstant(millis: Long, zoneId: ZoneId): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(zoneId)
        .toInstant()
        .toString()

internal fun utcIsoToLocalDate(value: String, zoneId: ZoneId): String =
    parseUtcInstant(value)?.atZone(zoneId)?.toLocalDate()?.toString()
        ?: value.substringBefore('T').ifBlank { value }

private fun parseUtcInstant(value: String): Instant? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    runCatching { Instant.parse(trimmed) }.getOrNull()?.let { return it }
    return runCatching { Instant.parse("${trimmed}Z") }.getOrNull()
}

private val visitDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

internal actual fun formatVisitDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(visitDateFormatter)
