package com.coffeepeek.admin.di.coffee

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.coffee.api.CoffeeAnalytics
import com.coffeepeek.feature.coffee.api.CoffeeContentAction
import com.coffeepeek.feature.coffee.api.CoffeeDetailAnalytics
import com.coffeepeek.feature.coffee.api.CoffeeDetailSection
import com.coffeepeek.feature.coffee.api.CoffeeExposure
import com.coffeepeek.feature.coffee.api.CoffeeListAnalytics
import com.coffeepeek.feature.coffee.api.CoffeeScreenAnalytics

/** Same consent-aware trackers and event names as the legacy catalogue screens, not a second analytics service. */
internal object AppCoffeeAnalytics : CoffeeAnalytics {
    override fun list(): CoffeeListAnalytics = AppCoffeeListAnalytics()
    override fun detail(): CoffeeDetailAnalytics = AppCoffeeDetailAnalytics
}

private class AppCoffeeListAnalytics : CoffeeListAnalytics {
    // One tracker per list ViewModel: it deduplicates unchanged criteria and settles each attempt once.
    private val tracker = SearchTracker(Catalog.Coffees)
    private var attempt: SearchAttempt? = null

    override fun searchStarted(fingerprint: String, query: String, filterKeys: List<String>) {
        attempt = tracker.begin(fingerprint, query, filterKeys)
    }
    override fun resultsViewed(count: Int) { attempt?.results(count) }
    override fun searchFailed(error: Throwable) { attempt?.failed(safeAnalyticsError(error)) }
    override fun opened(position: Int) = tracker.open(Entity.Coffee, position)
    override fun filtersOpened() {
        Analytics.track(AnalyticsEvent.FILTERS_OPENED, "catalog" to "coffees", "placement" to "mobile")
    }
    override fun filtersApplied(filterKeys: List<String>, changedKeys: List<String>, reset: Boolean) {
        Analytics.track(AnalyticsEvent.FILTERS_APPLIED, "catalog" to "coffees", "filter_keys" to filterKeys.sorted(),
            "changed_filter_keys" to changedKeys.sorted(), "action" to if (reset) "reset" else "apply")
    }
}

private object AppCoffeeDetailAnalytics : CoffeeDetailAnalytics {
    override fun contentInteracted(section: CoffeeDetailSection, action: CoffeeContentAction) {
        Analytics.track(AnalyticsEvent.ENTITY_CONTENT_INTERACTED, "entity_type" to "coffee", "section" to section.wire(),
            "action" to action.wire())
    }
    override fun roasterOpened() = DetailEntryMemory.record(Entity.Roaster, ResultSource.Related, null)
    override fun similarOpened() = DetailEntryMemory.record(Entity.Coffee, ResultSource.Related, null)
}

private fun CoffeeDetailSection.wire() = when (this) {
    CoffeeDetailSection.Photos -> "photos"
    CoffeeDetailSection.Description -> "description"
    CoffeeDetailSection.Roaster -> "roaster"
    CoffeeDetailSection.SimilarCoffees -> "similar_coffees"
    CoffeeDetailSection.Contacts -> "contacts"
}

private fun CoffeeContentAction.wire() = when (this) {
    CoffeeContentAction.Open -> "open"
    CoffeeContentAction.Close -> "close"
    CoffeeContentAction.Select -> "select"
    CoffeeContentAction.Share -> "share"
}

internal object AppCoffeeScreenAnalytics : CoffeeScreenAnalytics {
    @Composable
    override fun TrackDetail(loaded: Boolean) = TrackEntityDetail(Entity.Coffee, loaded)

    @Composable
    override fun Overlay(content: @Composable () -> Unit) = AnalyticsOverlay(content)

    @Composable
    override fun exposure(modifier: Modifier, section: CoffeeExposure): Modifier {
        val (feature, placement, page) = section.tracking()
        return modifier.analyticsExposure(feature, placement, page)
    }
}

internal fun CoffeeExposure.tracking(): Triple<String, String, String> = when (this) {
    CoffeeExposure.Catalog -> Triple("coffee_catalog", "catalog", "coffees")
    CoffeeExposure.Photos -> Triple("coffee_photos", "coffee_detail", "coffee_detail")
    CoffeeExposure.Roaster -> Triple("coffee_roaster", "coffee_detail", "coffee_detail")
    CoffeeExposure.Offers -> Triple("coffee_offers", "coffee_detail", "coffee_detail")
    CoffeeExposure.SimilarCoffees -> Triple("similar_coffees", "coffee_detail", "coffee_detail")
    CoffeeExposure.Search -> Triple("search", "coffees", "coffees")
    CoffeeExposure.Filters -> Triple("filters", "coffees", "coffees")
}
