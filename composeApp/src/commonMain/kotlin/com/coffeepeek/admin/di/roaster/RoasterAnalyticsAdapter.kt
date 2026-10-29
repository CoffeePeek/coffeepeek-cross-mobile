package com.coffeepeek.admin.di.roaster

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.roaster.api.RoasterAnalytics
import com.coffeepeek.feature.roaster.api.RoasterContentAction
import com.coffeepeek.feature.roaster.api.RoasterDetailAnalytics
import com.coffeepeek.feature.roaster.api.RoasterDetailSection
import com.coffeepeek.feature.roaster.api.RoasterExposure
import com.coffeepeek.feature.roaster.api.RoasterListAnalytics
import com.coffeepeek.feature.roaster.api.RoasterScreenAnalytics

/** Same consent-aware trackers and event names as the legacy roaster screens, not a second analytics service. */
internal object RoasterAnalyticsAdapter : RoasterAnalytics {
    override fun list(): RoasterListAnalytics = RoasterListAnalyticsAdapter()
    override fun detail(): RoasterDetailAnalytics = RoasterDetailAnalyticsAdapter
}

private class RoasterListAnalyticsAdapter : RoasterListAnalytics {
    // One tracker per list ViewModel: it deduplicates unchanged criteria and settles each attempt once.
    private val tracker = SearchTracker(Catalog.Roasters)

    override fun resultsShown(fingerprint: String, query: String, filterKeys: List<String>, resultCount: Int, failed: Boolean) {
        val attempt = tracker.begin(fingerprint, query, filterKeys)
        if (failed) attempt?.failed(SafeAnalyticsError("unknown")) else attempt?.results(resultCount)
    }
    override fun opened(position: Int) = tracker.open(Entity.Roaster, position)
}

private object RoasterDetailAnalyticsAdapter : RoasterDetailAnalytics {
    override fun contentInteracted(section: RoasterDetailSection, action: RoasterContentAction) {
        Analytics.track(AnalyticsEvent.ENTITY_CONTENT_INTERACTED, "entity_type" to "roaster", "section" to section.wire(), "action" to action.wire())
    }
    override fun shopOpened() = DetailEntryMemory.record(Entity.Shop, ResultSource.Related, null)
}

private fun RoasterDetailSection.wire() = when (this) {
    RoasterDetailSection.Photos -> "photos"
    RoasterDetailSection.Shops -> "shops"
    RoasterDetailSection.Contacts -> "contacts"
}

private fun RoasterContentAction.wire() = when (this) {
    RoasterContentAction.Open -> "open"
    RoasterContentAction.Select -> "select"
    RoasterContentAction.Instagram -> "instagram"
    RoasterContentAction.Website -> "website"
}

internal object RoasterScreenAnalyticsAdapter : RoasterScreenAnalytics {
    @Composable
    override fun TrackDetail(loaded: Boolean) = TrackEntityDetail(Entity.Roaster, loaded)

    @Composable
    override fun exposure(modifier: Modifier, section: RoasterExposure): Modifier = when (section) {
        RoasterExposure.Search -> modifier.analyticsExposure("search", "roasters", "roasters")
        RoasterExposure.Catalog -> modifier.analyticsExposure("roaster_catalog", "catalog", "roasters")
        RoasterExposure.Discovery -> modifier.analyticsExposure("roaster_catalog", "discovery", "discovery")
        RoasterExposure.Photos -> modifier.analyticsExposure("roaster_photos", "roaster_detail", "roaster_detail")
        RoasterExposure.Contacts -> modifier.analyticsExposure("roaster_contacts", "roaster_detail", "roaster_detail")
        RoasterExposure.Shops -> modifier.analyticsExposure("roaster_shops", "roaster_detail", "roaster_detail")
        // The legacy favorite button chose its placement from the current page, for card and detail alike.
        RoasterExposure.FavoriteCard, RoasterExposure.FavoriteDetail -> modifier.favoriteExposure()
    }
}

@Composable
private fun Modifier.favoriteExposure(): Modifier {
    val page = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    val placement = when (page) { "shop_detail" -> "shop_detail"; "roaster_detail" -> "roaster_card"; else -> "catalog_card" }
    return analyticsExposure("favorites", placement, page)
}
