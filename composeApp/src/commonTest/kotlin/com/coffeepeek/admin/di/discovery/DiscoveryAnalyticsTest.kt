@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.discovery.api.DiscoveryGate
import com.coffeepeek.feature.discovery.api.FilterAction
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.api.MapInteraction
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** In-memory telemetry only; the shared adapter never installs or sends to a vendor SDK. */
class DiscoveryAnalyticsTest {
    @Test fun listAndMapKeepSeparateSearchCorrelationsWithoutSendingTheQuery() = runTest {
        val sdk = installDiscoveryAnalytics()
        val port = AppDiscoveryAnalytics()
        port.searchStarted("private coffee query", listOf("city"), false, "minsk", "list").results(2)
        port.mapSearchStarted("private map query", emptyList(), "minsk", "map").results(1)
        port.shopOpened(2)
        port.mapShopOpened(true)
        runCurrent()
        val searches = sdk.events.filter { it.first == "catalog_search_started" }.map { it.second }
        assertEquals(2, searches.size)
        assertNotEquals(searches[0]["search_id"], searches[1]["search_id"])
        val opens = sdk.events.filter { it.first == "catalog_item_opened" }.map { it.second }
        assertEquals(searches.map { it["search_id"] }, opens.map { it["search_id"] })
        assertEquals("map", opens[1]["source"])
        assertTrue(sdk.events.none { "private" in it.second.toString() })
    }

    @Test fun filtersLocationAndMapKeepTheirLegacyProperties() = runTest {
        val sdk = installDiscoveryAnalytics()
        val port = AppDiscoveryAnalytics()
        port.filtersChanged(listOf("city"), listOf("city"), FilterAction.Reset)
        port.gateShown(DiscoveryGate.FavoritesFilter)
        port.geolocation("map", true, GeolocationOutcome.Unavailable)
        port.mapInteracted(MapInteraction.ZonesToggled, enabled = false)
        runCurrent()
        assertEquals("reset", sdk.events.single { it.first == "filters_applied" }.second["action"])
        assertEquals("favorites_filter", sdk.events.single { it.first == "auth_gate_shown" }.second["feature"])
        val location = sdk.events.single { it.first == "geolocation_result" }.second
        assertEquals("user", location["trigger"])
        assertEquals("unavailable", location["outcome"])
        val map = sdk.events.single { it.first == "map_interaction" }.second
        assertEquals("zones_toggled", map["action"])
        assertEquals(false, map["enabled"])
    }

    @Test fun declinedConsentSuppressesDiscoveryTelemetry() = runTest {
        val sdk = installDiscoveryAnalytics(AnalyticsConsent.Declined)
        val port = AppDiscoveryAnalytics()
        port.searchStarted("private query", emptyList(), false, null, "criteria").results(1)
        port.filtersOpened()
        port.mapInteracted(MapInteraction.Expanded)
        runCurrent()
        assertTrue(sdk.events.isEmpty())
    }
}

private suspend fun TestScope.installDiscoveryAnalytics(consent: AnalyticsConsent = AnalyticsConsent.Accepted): RecordingTransport {
    val sdk = RecordingTransport()
    val service = analytics(MemoryConsent(consent), sdk)
    service.setForeground(true)
    service.restoreConsent()
    service.syncIdentity("fixture-owner")
    service.screenShown("discovery", "fixture-visit")
    Analytics.install(service)
    return sdk
}
