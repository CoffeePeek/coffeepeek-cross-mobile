package com.coffeepeek.admin.location

internal enum class LocationPermissionAction { Granted, Request, Settings }
internal enum class LocationPermissionOutcome { Granted, Denied, PermanentlyDenied }

/** A previous denial is permanent only when Android also declines to show its rationale. */
internal fun locationPermissionAction(granted: Boolean, requestedBefore: Boolean, canAskAgain: Boolean) = when {
    granted -> LocationPermissionAction.Granted
    requestedBefore && !canAskAgain -> LocationPermissionAction.Settings
    else -> LocationPermissionAction.Request
}

internal fun locationPermissionOutcome(granted: Boolean, canAskAgain: Boolean) = when {
    granted -> LocationPermissionOutcome.Granted
    canAskAgain -> LocationPermissionOutcome.Denied
    else -> LocationPermissionOutcome.PermanentlyDenied
}

/** A live request may observe Settings changes; an unrequested or disposed source stays inert. */
internal class LocationPermissionRequest {
    private var key = 0
    private var outcome: LocationPermissionOutcome? = null
    private var closed = false

    fun start(next: Int): Boolean {
        if (closed || next <= key) return false
        key = next
        outcome = null
        return true
    }

    fun deliver(next: LocationPermissionOutcome, active: Boolean = true): Boolean {
        if (closed || !active || key <= 0 || outcome == next) return false
        outcome = next
        return true
    }

    fun close() {
        closed = true
        outcome = null
    }
}
