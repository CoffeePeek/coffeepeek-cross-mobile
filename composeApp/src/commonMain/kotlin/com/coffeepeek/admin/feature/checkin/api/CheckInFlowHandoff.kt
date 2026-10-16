package com.coffeepeek.admin.feature.checkin.api

import com.coffeepeek.admin.analytics.CheckInTracker

/** One in-memory creation intent across type picker -> shop picker -> editor. */
internal object CheckInFlowHandoff {
    var tracker: CheckInTracker? = null
        set(value) { field = value; continuing = false }
    private var continuing = false
    fun continueToShop() { continuing = true }
    fun cancelFromBack() { if (!continuing) take()?.cancel("back") }
    fun take(): CheckInTracker? = tracker?.takeIf { it.isFresh }.also { tracker = null }
}
