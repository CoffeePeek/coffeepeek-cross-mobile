package com.coffeepeek.admin.ui.component

/** A positive provisional height lets Compose mount before its first content measurement. */
internal fun nativeSheetDetentHeight(contentHeight: Double, maximumHeight: Double, heightFraction: Float): Double {
    val limit = (maximumHeight * heightFraction.coerceIn(0.1f, 1f)).coerceAtLeast(1.0)
    return if (contentHeight > 0.0) contentHeight.coerceAtMost(limit) else limit / 2
}
