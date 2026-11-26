package com.coffeepeek.admin.notifications

import kotlinx.coroutines.delay

/** The root foreground child owns this loop; cancelling it stops the next refresh. */
internal suspend fun foregroundNotificationHeartbeat(foreground: () -> Unit) {
    while (true) {
        foreground()
        delay(15 * 60 * 1_000L)
    }
}
