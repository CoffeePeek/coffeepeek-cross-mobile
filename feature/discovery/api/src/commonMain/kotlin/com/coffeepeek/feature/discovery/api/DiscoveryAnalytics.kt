package com.coffeepeek.feature.discovery.api

/** One search whose outcome is reported: the application backs it with its search tracker. */
interface SearchRun {
    fun results(count: Int)
    fun failed(error: Throwable)
}

enum class FilterAction { Apply, Reset }

/** The two features that need an account and send a guest to sign in. */
enum class DiscoveryGate { VisitedFilter, FavoritesFilter }

/** Where a geolocation answer was asked and what it was. */
enum class GeolocationOutcome { Available, Unavailable }

/**
 * What Discovery reports. The application maps it to its analytics with the event names and properties of the legacy screens; the owner knows no vendor.
 * [filterKeys] are the property keys of the filters in use (`city`, `type`, `isOpen`, ...), [changedKeys] the ones whose value changed.
 */
interface DiscoveryAnalytics {
    fun searchStarted(text: String, filterKeys: List<String>, nearby: Boolean, citySlug: String?): SearchRun
    fun shopOpened(position: Int?)
    fun filtersOpened()
    fun filtersChanged(filterKeys: List<String>, changedKeys: List<String>, action: FilterAction)
    fun gateShown(gate: DiscoveryGate)
    fun geolocation(placement: String, userTriggered: Boolean, outcome: GeolocationOutcome)
}

/** Reports nothing; for previews and tests. */
object NoDiscoveryAnalytics : DiscoveryAnalytics {
    private object NoRun : SearchRun {
        override fun results(count: Int) = Unit
        override fun failed(error: Throwable) = Unit
    }

    override fun searchStarted(text: String, filterKeys: List<String>, nearby: Boolean, citySlug: String?): SearchRun = NoRun
    override fun shopOpened(position: Int?) = Unit
    override fun filtersOpened() = Unit
    override fun filtersChanged(filterKeys: List<String>, changedKeys: List<String>, action: FilterAction) = Unit
    override fun gateShown(gate: DiscoveryGate) = Unit
    override fun geolocation(placement: String, userTriggered: Boolean, outcome: GeolocationOutcome) = Unit
}
