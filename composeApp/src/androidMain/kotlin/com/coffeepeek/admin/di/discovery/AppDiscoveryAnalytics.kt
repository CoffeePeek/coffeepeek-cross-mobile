package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.Catalog
import com.coffeepeek.admin.analytics.Entity
import com.coffeepeek.admin.analytics.ResultSource
import com.coffeepeek.admin.analytics.SearchAttempt
import com.coffeepeek.admin.analytics.SearchTracker
import com.coffeepeek.admin.analytics.correlationId
import com.coffeepeek.admin.analytics.safeAnalyticsError
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryGate
import com.coffeepeek.feature.discovery.api.DiscoveryOverlay
import com.coffeepeek.feature.discovery.api.FilterAction
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.api.MapInteraction
import com.coffeepeek.feature.discovery.api.SearchRun

/** The names and properties of the legacy discovery events; the owner knows no vendor. One instance serves the search and the map (each has its own search tracker). */
internal class AppDiscoveryAnalytics : DiscoveryAnalytics {
    private val listSearches = SearchTracker(Catalog.Shops)
    private val mapSearches = SearchTracker(Catalog.Shops, ResultSource.Map)

    private class Run(private val attempt: SearchAttempt?) : SearchRun {
        override fun results(count: Int) { attempt?.results(count) }
        override fun failed(error: Throwable) { attempt?.failed(safeAnalyticsError(error)) }
    }

    override fun searchStarted(text: String, filterKeys: List<String>, nearby: Boolean, citySlug: String?, criteria: Any): SearchRun =
        Run(listSearches.begin(criteria, text, filterKeys, nearby, citySlug = citySlug))

    override fun mapSearchStarted(text: String, filterKeys: List<String>, citySlug: String?, criteria: Any): SearchRun =
        Run(mapSearches.begin(criteria, text, filterKeys))

    override fun shopOpened(position: Int?) = listSearches.open(Entity.Shop, position)
    override fun mapShopOpened(fromSearch: Boolean) = mapSearches.open(Entity.Shop, includeSearch = fromSearch)

    override fun filtersOpened() {
        Analytics.track(AnalyticsEvent.FILTERS_OPENED, "catalog" to "shops", "placement" to "mobile")
    }

    override fun filtersChanged(filterKeys: List<String>, changedKeys: List<String>, action: FilterAction) {
        Analytics.track(
            AnalyticsEvent.FILTERS_APPLIED, "catalog" to "shops", "filter_keys" to filterKeys, "changed_filter_keys" to changedKeys,
            "action" to if (action == FilterAction.Reset) "reset" else "apply",
        )
    }

    override fun gateShown(gate: DiscoveryGate) {
        Analytics.gate(if (gate == DiscoveryGate.VisitedFilter) "visited_filter" else "favorites_filter")
    }

    override fun geolocation(placement: String, userTriggered: Boolean, outcome: GeolocationOutcome) {
        Analytics.track(
            AnalyticsEvent.GEOLOCATION_RESULT, "placement" to placement, "trigger" to if (userTriggered) "user" else "automatic",
            "outcome" to if (outcome == GeolocationOutcome.Available) "available" else "unavailable",
        )
    }

    override fun mapInteracted(action: MapInteraction, resultCount: Int?, durationMs: Long?, enabled: Boolean?) {
        val properties = buildList {
            add("action" to action.wire)
            resultCount?.let { add("result_count" to it) }
            durationMs?.let { add("duration_ms" to it) }
            enabled?.let { add("enabled" to it) }
        }
        Analytics.track(AnalyticsEvent.MAP_INTERACTION, *properties.toTypedArray())
    }

    override fun overlayShown(overlay: DiscoveryOverlay) {
        Analytics.service?.screenShown(overlay.name.lowercase(), correlationId("screen"))
    }
}

private val MapInteraction.wire: String
    get() = when (this) {
        MapInteraction.MarkerSelected -> "marker_selected"
        MapInteraction.SearchSelected -> "search_selected"
        MapInteraction.Zoomed -> "zoomed"
        MapInteraction.ZonesToggled -> "zones_toggled"
        MapInteraction.Loaded -> "loaded"
        MapInteraction.LoadFailed -> "load_failed"
        MapInteraction.NearbyLoaded -> "nearby_loaded"
        MapInteraction.NearbyFailed -> "nearby_failed"
        MapInteraction.Expanded -> "expanded"
        MapInteraction.Collapsed -> "collapsed"
    }
