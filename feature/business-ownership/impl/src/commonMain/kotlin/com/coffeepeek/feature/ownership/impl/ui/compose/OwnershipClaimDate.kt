package com.coffeepeek.feature.ownership.impl.ui.compose

import com.coffeepeek.core.datetime.parseServerInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

private val claimDateFormat = LocalDate.Format {
    day()
    chars(".")
    monthNumber()
    chars(".")
    year()
}

private val claimTimestampFormat = LocalDateTime.Format {
    date(claimDateFormat)
    chars(", ")
    hour()
    chars(":")
    minute()
}

@OptIn(ExperimentalTime::class)
internal fun formatOwnershipClaimDate(raw: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): String? {
    val instant = parseServerInstant(raw)
    if (instant != null) return claimTimestampFormat.format(instant.toLocalDateTime(timeZone))

    val date = runCatching { LocalDate.parse(raw.trim()) }.getOrNull() ?: return null
    return claimDateFormat.format(date)
}
