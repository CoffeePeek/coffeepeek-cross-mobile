@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.coffee.api.CoffeeContentAction
import com.coffeepeek.feature.coffee.api.CoffeeDetailSection
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.*

/** Adapter parity with the legacy catalogue events (see CoffeeFilterAnalyticsTest for the legacy ViewModel). */
class AndroidCoffeeAnalyticsTest {
    private suspend fun kotlinx.coroutines.test.TestScope.installed(): RecordingTransport {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("backend-id"); Analytics.install(service)
        runCurrent()
        return sdk
    }

    @Test fun searchLifecycleMatchesLegacyEventsAndDeduplicatesUnchangedCriteria() = runTest {
        val sdk = installed()
        val list = AndroidCoffeeAnalytics.list()
        list.searchStarted("||false", "", emptyList())
        list.searchStarted("||false", "", emptyList()) // refresh with unchanged criteria
        list.resultsViewed(3)
        list.resultsViewed(4) // settled once
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "catalog_search_started" })
        val started = sdk.events.single { it.first == "catalog_search_started" }.second
        assertEquals("coffees", started["catalog"]); assertEquals(false, started["has_query"])
        assertEquals(3, sdk.events.single { it.first == "catalog_results_viewed" }.second["result_count"])
        list.opened(2)
        runCurrent()
        val opened = sdk.events.single { it.first == "catalog_item_opened" }.second
        assertEquals("coffee", opened["entity_type"]); assertEquals(2, opened["position"])
        assertEquals(started["search_id"], opened["search_id"])
    }

    @Test fun failureIsCategorizedAndSettlesTheAttempt() = runTest {
        val sdk = installed()
        val list = AndroidCoffeeAnalytics.list()
        list.searchStarted("q", "q", listOf("roast"))
        list.searchFailed(IOException("private payload"))
        list.resultsViewed(1)
        runCurrent()
        val failed = sdk.events.single { it.first == "catalog_search_failed" }.second
        assertEquals("network", failed["failure_category"])
        assertTrue(sdk.events.none { it.first == "catalog_results_viewed" })
        assertEquals(listOf("roast"), sdk.events.single { it.first == "catalog_search_started" }.second["filter_keys"])
    }

    @Test fun filterEventsMatchLegacyShapeAndNeverCarryValues() = runTest {
        val sdk = installed()
        val list = AndroidCoffeeAnalytics.list()
        list.filtersOpened()
        list.filtersApplied(listOf("roast", "availableOnly"), listOf("roast", "availableOnly"), reset = false)
        list.filtersApplied(emptyList(), listOf("roast"), reset = true)
        runCurrent()
        assertEquals(mapOf("catalog" to "coffees", "placement" to "mobile"), sdk.events.single { it.first == "filters_opened" }.second.filterKeys(setOf("catalog", "placement")))
        val applied = sdk.events.filter { it.first == "filters_applied" }.map { it.second }
        assertEquals(listOf("availableOnly", "roast"), applied[0]["filter_keys"]); assertEquals("apply", applied[0]["action"])
        assertEquals(emptyList<String>(), applied[1]["filter_keys"]); assertEquals("reset", applied[1]["action"])
    }

    @Test fun detailInteractionsUseLegacySectionAndActionNames() = runTest {
        val sdk = installed()
        val detail = AndroidCoffeeAnalytics.detail()
        detail.contentInteracted(CoffeeDetailSection.SimilarCoffees, CoffeeContentAction.Select)
        detail.contentInteracted(CoffeeDetailSection.Contacts, CoffeeContentAction.Share)
        detail.contentInteracted(CoffeeDetailSection.Description, CoffeeContentAction.Close)
        runCurrent()
        val rows = sdk.events.filter { it.first == "entity_content_interacted" }.map { it.second["section"] to it.second["action"] }
        assertEquals(listOf("similar_coffees" to "select", "contacts" to "share", "description" to "close"), rows)
        assertTrue(sdk.events.all { it.second["entity_type"] == "coffee" })
    }

    private fun Map<String, Any>.filterKeys(keys: Set<String>) = filter { it.key in keys }
}
