package com.coffeepeek.admin.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** A queued completion belongs to the root instance and account lifetime which opened its editor. */
internal class RootNavigationOrigin(private val session: RootNavigationSession) {
    fun accepts(event: Navigator.NavEvent, current: RootNavigationSession): Boolean =
        event !is Navigator.NavEvent.ReturnToProfile || event.origin === this && session == current
}

internal val LocalRootNavigationOrigin = staticCompositionLocalOf<RootNavigationOrigin> {
    error("Root navigation must provide its completion origin")
}
