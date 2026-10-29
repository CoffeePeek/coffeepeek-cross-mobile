package com.coffeepeek.feature.profile.impl.ui.compose

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSISO8601DateFormatter

/** The server may send fractional seconds or not; either is read, and the result follows the device locale and zone. */
internal actual fun formatUsernameRetryAt(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val parser = NSISO8601DateFormatter()
    val date = parser.dateFromString(value) ?: parser.apply {
        formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds
    }.dateFromString(value) ?: return null
    return NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }.stringFromDate(date)
}
