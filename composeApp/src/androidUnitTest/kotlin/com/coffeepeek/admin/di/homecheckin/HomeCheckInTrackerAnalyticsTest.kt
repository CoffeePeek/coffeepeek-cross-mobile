package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeCheckInTrackerAnalyticsTest {
    @Test fun inheritedGlobalFlowStaysOneAndNeverCancelsAfterSaved() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner"); Analytics.install(service)
        try {
            val inherited = CheckInTracker(CheckInKind.Undecided, CheckInSource.Global)
            inherited.kind(CheckInKind.Home)
            val adapter = AppHomeCheckInAnalytics({ true }, false, CheckInSource.Global, inherited)
            adapter.record(HomeCheckInEditorMetric.Opened); adapter.record(HomeCheckInEditorMetric.Opened)
            adapter.record(HomeCheckInEditorMetric.FieldChanged(HomePreparationField.Note))
            adapter.record(HomeCheckInEditorMetric.FieldChanged(HomePreparationField.Note))
            adapter.record(HomeCheckInEditorMetric.Submitted)
            adapter.record(HomeCheckInEditorMetric.Saved(HomePreparationVisibility.Private, true))
            adapter.record(HomeCheckInEditorMetric.Saved(HomePreparationVisibility.Private, true))
            adapter.record(HomeCheckInEditorMetric.Cancelled); runCurrent()
            assertEquals(1, sdk.events.count { it.first == "check_in_flow_started" })
            assertEquals(1, sdk.events.count { it.first == "check_in_created" })
            assertEquals(1, sdk.events.count { it.first == "check_in_saved" })
            assertEquals(1, sdk.events.count { it.first == "check_in_field_interacted" })
            assertEquals(0, sdk.events.count { it.first == "check_in_cancelled" })
            assertEquals(1, sdk.events.mapNotNull { it.second["flow_id"] }.toSet().size)
            assertTrue(sdk.events.none { "fixture-owner" in it.second.values })
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }

    @Test fun editMapsValidationAndSafeFailureButNeverRecordsCreated() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner"); Analytics.install(service)
        try {
            val adapter = AppHomeCheckInAnalytics({ true }, true, CheckInSource.Diary)
            adapter.record(HomeCheckInEditorMetric.Opened)
            adapter.record(HomeCheckInEditorMetric.Submitted)
            adapter.record(HomeCheckInEditorMetric.Validation(setOf(HomePreparationField.Bean), true))
            adapter.record(HomeCheckInEditorMetric.Failed(HomeCheckInEditorStage.Photo, HomePreparationIssue.Offline))
            adapter.record(HomeCheckInEditorMetric.Saved(null, false)); runCurrent()
            assertEquals(0, sdk.events.count { it.first == "check_in_created" })
            assertEquals("server", sdk.events.single { it.first == "check_in_validation_failed" }.second["validation_source"])
            assertEquals("photo_upload", sdk.events.single { it.first == "check_in_failed" }.second["stage"])
            assertEquals("network", sdk.events.single { it.first == "check_in_failed" }.second["error_category"])
            assertEquals("unknown", sdk.events.single { it.first == "check_in_saved" }.second["visibility"])
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }

    @Test fun beanSearchFailureKeepsItsHttpStatusAndValidationCategory() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner"); Analytics.install(service)
        try {
            val adapter = AppHomeCheckInAnalytics({ true }, false, CheckInSource.Direct)
            adapter.record(HomeCheckInEditorMetric.Opened)
            adapter.record(HomeCheckInEditorMetric.Failed(HomeCheckInEditorStage.Bean, HomePreparationIssue.RequestFailed, 422))
            adapter.record(HomeCheckInEditorMetric.Failed(HomeCheckInEditorStage.Bean, HomePreparationIssue.RequestFailed)); runCurrent()
            val failures = sdk.events.filter { it.first == "check_in_failed" }.map { it.second }
            assertEquals("validation", failures[0]["error_category"])
            assertEquals(422, failures[0]["status_code"])
            assertEquals("unknown", failures[1]["error_category"])
            assertNull(failures[1]["status_code"])
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }

    @Test fun consentDeclinedAtStartCannotReplayThisFlowAfterAcceptance() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Declined), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner"); Analytics.install(service)
        try {
            val adapter = AppHomeCheckInAnalytics({ true }, false, CheckInSource.Direct)
            adapter.record(HomeCheckInEditorMetric.Opened)
            service.setConsent(AnalyticsConsent.Accepted)
            adapter.record(HomeCheckInEditorMetric.Submitted)
            adapter.record(HomeCheckInEditorMetric.Saved(HomePreparationVisibility.Public, false)); runCurrent()
            assertTrue(sdk.events.isEmpty())
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }

    @Test fun revocationStopsMetricsAndCancelIsTerminal() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("fixture-owner"); Analytics.install(service)
        try {
            var current = true
            val adapter = AppHomeCheckInAnalytics({ current }, false, CheckInSource.Direct)
            adapter.record(HomeCheckInEditorMetric.Opened); runCurrent()
            val before = sdk.events.toList()
            current = false
            adapter.record(HomeCheckInEditorMetric.Saved(HomePreparationVisibility.Private, false))
            adapter.record(HomeCheckInEditorMetric.Cancelled); runCurrent()
            assertEquals(before, sdk.events)
            val closed = AppHomeCheckInAnalytics({ true }, false, CheckInSource.Direct)
            closed.record(HomeCheckInEditorMetric.Opened)
            closed.record(HomeCheckInEditorMetric.Cancelled); closed.record(HomeCheckInEditorMetric.Cancelled)
            closed.record(HomeCheckInEditorMetric.Submitted); runCurrent()
            assertEquals(1, sdk.events.count { it.first == "check_in_cancelled" })
            assertEquals(0, sdk.events.count { it.first == "check_in_created" })
        } finally { service.setConsent(AnalyticsConsent.Declined) }
    }
}
