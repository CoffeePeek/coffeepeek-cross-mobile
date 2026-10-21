@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.roaster.api.RoasterContentAction
import com.coffeepeek.feature.roaster.api.RoasterDetailSection
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Adapter parity with the legacy roaster events (same trackers, event names and properties). */
class RoasterAnalyticsAdapterTest {
    private suspend fun TestScope.installed(): RecordingTransport {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("backend-id"); Analytics.install(service)
        runCurrent()
        return sdk
    }

    @Test fun searchEventsUseTheRoasterCatalogAndDeduplicateUnchangedCriteria() = runTest {
        val sdk = installed()
        val list = RoasterAnalyticsAdapter.list()
        list.resultsShown("a||false|", "", emptyList(), 3, failed = false)
        list.resultsShown("a||false|", "", emptyList(), 3, failed = false) // same criteria again
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "catalog_search_started" })
        val started = sdk.events.single { it.first == "catalog_search_started" }.second
        assertEquals("roasters", started["catalog"])
        assertEquals(3, sdk.events.single { it.first == "catalog_results_viewed" }.second["result_count"])
        list.opened(2); runCurrent()
        val opened = sdk.events.single { it.first == "catalog_item_opened" }.second
        assertEquals(mapOf("entity_type" to "roaster", "position" to 2, "search_id" to started["search_id"]),
            opened.filterKeys { it in setOf("entity_type", "position", "search_id") })
    }

    @Test fun failedSearchIsReportedOnceAsUnknownAndFilterKeysAreKept() = runTest {
        val sdk = installed()
        val list = RoasterAnalyticsAdapter.list()
        list.resultsShown("q|r||", "q", listOf("roasters", "tags", "favoritesOnly"), 0, failed = true)
        list.resultsShown("q|r||", "q", listOf("roasters"), 5, failed = false)
        runCurrent()
        val failed = sdk.events.single { it.first == "catalog_search_failed" }.second
        assertEquals("unknown", failed["failure_category"])
        assertTrue(sdk.events.none { it.first == "catalog_results_viewed" })
        assertEquals(listOf("favoritesOnly", "roasters", "tags"), sdk.events.single { it.first == "catalog_search_started" }.second["filter_keys"])
    }

    @Test fun detailInteractionsUseLegacySectionAndActionNames() = runTest {
        val sdk = installed()
        val detail = RoasterAnalyticsAdapter.detail()
        detail.contentInteracted(RoasterDetailSection.Photos, RoasterContentAction.Open)
        detail.contentInteracted(RoasterDetailSection.Shops, RoasterContentAction.Select)
        detail.contentInteracted(RoasterDetailSection.Contacts, RoasterContentAction.Instagram)
        detail.contentInteracted(RoasterDetailSection.Contacts, RoasterContentAction.Website)
        runCurrent()
        val rows = sdk.events.filter { it.first == "entity_content_interacted" }.map { it.second["section"] to it.second["action"] }
        assertEquals(listOf("photos" to "open", "shops" to "select", "contacts" to "instagram", "contacts" to "website"), rows)
        assertTrue(sdk.events.all { it.second["entity_type"] == "roaster" })
    }

    @Test fun openingAShopAttributesItsEntryToTheRoaster() = runTest {
        installed()
        RoasterAnalyticsAdapter.detail().shopOpened()
        val entry = DetailEntryMemory.consume(Entity.Shop)
        assertEquals("related", entry["entry_point"])
        assertEquals("coffee_shop", entry["entity_type"])
    }
}
