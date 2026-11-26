package com.coffeepeek.admin.di.discovery

/** A newly granted permission updates the puck; only an explicit location request recentres an existing map. */
internal class IosDiscoveryLocationRequests {
    private var initial = true
    private var handled: Int? = null

    fun restore(request: Int) {
        initial = false
        handled = request
    }

    fun take(request: Int, permitted: Boolean): Boolean {
        if (initial && request == 0 && !permitted) handled = 0
        initial = false
        if (!permitted || handled == request) return false
        handled = request
        return true
    }
}
