package com.coffeepeek.feature.profile.impl.ui.compose

import com.coffeepeek.core.datetime.parseServerInstant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal actual fun localizedUsernameRetryAt(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val instant = parseServerInstant(value) ?: return null
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble()))
}
