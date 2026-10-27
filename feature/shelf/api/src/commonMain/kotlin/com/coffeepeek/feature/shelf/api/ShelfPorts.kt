package com.coffeepeek.feature.shelf.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Application image port for a protected bag card: loads it with the owner's session, fits it whole inside [modifier]
 * at [alignment] and owns the loading and failure look.
 */
typealias ShelfImage = @Composable (url: String, description: String?, alignment: Alignment, modifier: Modifier) -> Unit

/** The device time zone, so days on the shelf and archive months follow the user's calendar. */
fun interface ShelfTimeZone {
    /** Offset from UTC at the instant [epochSeconds], DST included. */
    fun offsetSeconds(epochSeconds: Long): Int

    companion object {
        val Utc = ShelfTimeZone { 0 }
    }
}
