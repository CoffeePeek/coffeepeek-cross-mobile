@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import kotlinx.coroutines.test.*
import kotlin.test.*

class AnalyticsTrackersTest {
    @Test fun searchesDedupeCriteriaExcludeStaleAndPreserveCorrelation() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity(null)
        Analytics.install(service)
        val tracker = SearchTracker(Catalog.Shops)
        val old = tracker.begin("old", "secret query", listOf("city"))
        val current = tracker.begin("new", "  a   😀  ", emptyList())
        assertSame(current, tracker.begin("new", "  a   😀  ", emptyList()))
        old?.results(100)
        current?.results(0)
        current?.results(5)
        tracker.open(Entity.Shop, 2)
        runCurrent()
        assertEquals(2, sdk.events.count { it.first == "catalog_search_started" })
        val result = sdk.events.single { it.first == "catalog_results_viewed" }.second
        assertEquals(4, result["query_length"])
        assertEquals(true, result["is_empty"])
        assertEquals(current?.id, result["search_id"])
        assertEquals(current?.id, sdk.events.single { it.first == "catalog_item_opened" }.second["search_id"])
        assertTrue(sdk.events.none { "secret query" in it.second.toString() || "😀" in it.second.toString() })
    }

    @Test fun searchUsesDifferentFailureKeysAndDoesNotCaptureCancellation() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity(null); Analytics.install(service)
        val tracker = SearchTracker(Catalog.Shops)
        val search = tracker.begin("1", "", emptyList())
        search?.failed(SafeAnalyticsError("cancelled"))
        search?.failed(SafeAnalyticsError("permission", 403))
        runCurrent()
        val result = sdk.events.single { it.first == "catalog_search_failed" }.second
        assertEquals("authorization", result["failure_category"])
        assertEquals(403, result["http_status"])
        assertFalse("status_code" in result || "error_category" in result)
    }

    @Test fun oneFlowAcrossValidationRetryAndSuccessAndEditHasNoCreated() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("user"); Analytics.install(service)
        val flow = CheckInTracker(CheckInKind.Undecided, CheckInSource.Global)
        flow.step(CheckInStep.Type); flow.step(CheckInStep.Type)
        flow.kind(CheckInKind.Home); flow.step(CheckInStep.Editor)
        flow.field("note"); flow.field("note")
        flow.submit(); flow.validation(listOf("note"))
        flow.submit(); flow.failed(CheckInStage.Save, SafeAnalyticsError("server", 503))
        flow.submit(); flow.saved("Private", true); flow.saved("Public", false); flow.cancel("close")
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "check_in_saved" })
        assertEquals(1, sdk.events.count { it.first == "check_in_created" })
        assertEquals(2, sdk.events.count { it.first == "check_in_step_viewed" })
        assertEquals(1, sdk.events.count { it.first == "check_in_field_interacted" })
        assertEquals(0, sdk.events.count { it.first == "check_in_cancelled" })
        val saved = sdk.events.single { it.first == "check_in_saved" }.second
        assertEquals(3, saved["attempt"])
        assertEquals(1, sdk.events.mapNotNull { it.second["flow_id"] }.toSet().size)
        val edit = CheckInTracker(CheckInKind.Visit, CheckInSource.Diary, edit = true)
        edit.submit(); edit.saved("Public", false); runCurrent()
        assertEquals(2, sdk.events.count { it.first == "check_in_saved" })
        assertEquals(1, sdk.events.count { it.first == "check_in_created" })
    }
}
