package com.coffeepeek.admin.utils

import com.coffeepeek.core.datetime.parseServerJavaInstant
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

internal actual fun instantToDatePickerMillis(millis: Long): Long =
    instantToDatePickerMillis(millis, ZoneId.systemDefault())

internal actual fun datePickerMillisWithLocalTime(dateMillis: Long, referenceMillis: Long): Long =
    datePickerMillisWithLocalTime(dateMillis, referenceMillis, ZoneId.systemDefault())

internal fun instantToDatePickerMillis(millis: Long, zoneId: ZoneId): Long =
    Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun datePickerMillisWithLocalTime(dateMillis: Long, referenceMillis: Long, zoneId: ZoneId): Long {
    val selected = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val reference = Instant.ofEpochMilli(referenceMillis).atZone(zoneId)
    // Retain the exact instant, including its offset during a repeated DST hour.
    if (selected == reference.toLocalDate()) return referenceMillis
    return selected.atTime(reference.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()
}

internal actual fun utcIsoToLocalDate(value: String): String =
    utcIsoToLocalDate(value, ZoneId.systemDefault())

internal actual fun utcIsoToLocalDateTime(value: String): String =
    utcIsoToLocalDateTime(value, ZoneId.systemDefault())

internal fun utcIsoToLocalDateTime(value: String, zoneId: ZoneId): String =
    parseServerJavaInstant(value)
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
    parseServerJavaInstant(value)?.atZone(zoneId)?.toLocalDate()?.toString()
        ?: value.substringBefore('T').ifBlank { value }

private val visitDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

internal actual fun formatVisitDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(visitDateFormatter)
