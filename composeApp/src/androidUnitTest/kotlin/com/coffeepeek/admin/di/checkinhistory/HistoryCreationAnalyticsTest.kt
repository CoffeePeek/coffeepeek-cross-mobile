package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.MemoryConsent
import com.coffeepeek.admin.analytics.RecordingTransport
import com.coffeepeek.admin.analytics.analytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryCreationAnalyticsTest {
    @Test fun homeHandoffPreservesOneFlowAndDoesNotCancelAfterSelectionOrSave() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner")
        Analytics.install(service)
        try {
            val flow = HistoryCreationAnalytics { true }
            flow.opened(); flow.opened()
            val tracker = requireNotNull(flow.home())
            assertNull(flow.home())
            flow.cancelled() // picker disposal must not cancel the handed-off Home tracker.
            tracker.submit(); tracker.saved("Private", false)
            runCurrent()
            assertEquals(1, sdk.events.count { it.first == "check_in_start_attempted" })
            assertEquals(1, sdk.events.count { it.first == "check_in_flow_started" })
            assertEquals(1, sdk.events.count { it.first == "check_in_kind_selected" })
            assertEquals(1, sdk.events.count { it.first == "check_in_created" })
            assertEquals(0, sdk.events.count { it.first == "check_in_cancelled" })
            assertEquals(1, sdk.events.mapNotNull { it.second["flow_id"] }.toSet().size)
            assertEquals("global_create", sdk.events.single { it.first == "check_in_created" }.second["source"])
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }

    @Test fun cancelIsDeduplicatedAndRevokedAccountCannotStartOrHandoff() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner")
        Analytics.install(service)
        try {
            var current = true
            val flow = HistoryCreationAnalytics { current }
            flow.opened(); flow.cancelled(); flow.cancelled(); runCurrent()
            assertEquals(1, sdk.events.count { it.first == "check_in_cancelled" })
            flow.opened(); runCurrent()
            current = false
            val before = sdk.events.toList()
            assertNull(flow.shop())
            flow.cancelled(); flow.opened(); runCurrent()
            assertEquals(before, sdk.events)
            assertTrue(sdk.events.none { "fixture-owner" in it.second.values })
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }

    @Test fun decliningConsentDoesNotQueueHistoryFlowForLaterAcceptance() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Declined), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner")
        Analytics.install(service)
        try {
            val flow = HistoryCreationAnalytics { true }
            flow.opened(); runCurrent()
            assertTrue(sdk.events.isEmpty())
            assertTrue(sdk.calls.isEmpty())
            service.setConsent(AnalyticsConsent.Accepted)
            val tracker = requireNotNull(flow.home())
            tracker.submit(); tracker.saved("Private", false); runCurrent()
            assertTrue(sdk.events.isEmpty())
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }
}
