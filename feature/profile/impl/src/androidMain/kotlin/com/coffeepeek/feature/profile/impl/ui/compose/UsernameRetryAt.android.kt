package com.coffeepeek.feature.profile.impl.ui.compose

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal actual fun localizedUsernameRetryAt(value: String?): String? =
    formatUsernameRetryAt(value, Locale.getDefault(), ZoneId.systemDefault())

internal fun formatUsernameRetryAt(value: String?, locale: Locale, zone: ZoneId): String? {
    if (value.isNullOrBlank()) return null
    return try {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale).withZone(zone).format(Instant.parse(value))
    } catch (_: DateTimeException) {
        null
    }
}
