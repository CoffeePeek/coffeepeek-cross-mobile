package com.coffeepeek.admin.utils

internal expect fun currentUtcIsoDateTime(): String

/** Current wall-clock time in epoch milliseconds. */
internal expect fun currentEpochMillis(): Long

/** Current local day of week using the API convention: Sunday = 0, Monday = 1. */
internal expect fun currentLocalDayOfWeek(): Int

internal expect fun currentLocalMinuteOfDay(): Int

/** Convert the calendar day selected in the local time zone to a UTC instant. */
internal expect fun datePickerMillisToUtcIsoInstant(millis: Long): String

/** Convert an ISO-8601 UTC instant from the backend to the device's local date. */
internal expect fun utcIsoToLocalDate(value: String): String

/** Convert an ISO-8601 UTC instant from the backend to local date and time. */
internal expect fun utcIsoToLocalDateTime(value: String): String

/** Human-readable visit date for the UI, e.g. "27 августа 2026". */
internal expect fun formatVisitDate(millis: Long): String
