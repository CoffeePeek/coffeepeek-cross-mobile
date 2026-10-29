@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import kotlinx.coroutines.test.*
import kotlin.test.*

/** In-memory transport only: no Application, SDK or production event delivery. */
class CheckInShopPickerAnalyticsTest {
    @AfterTest fun cleanup() { CheckInFlowHandoff.tracker = null }

    @Test fun selectionSurvivesTheNativeBackCallbackAndKeepsTheSameFlow() = runTest {
        val sdk = startTracker()
        val tracker = CheckInFlowHandoff.tracker
        val navigation = CheckInShopPickerNavigation({ true }, {}, {})
        navigation.select("coffee")
        CheckInFlowHandoff.cancelFromBack()
        assertSame(tracker, CheckInFlowHandoff.take())
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "check_in_flow_started" })
        assertTrue(sdk.events.none { it.first == "check_in_cancelled" })
    }

    @Test fun closeCancelsOnceEvenWhenNativeNavigationAlsoReportsBack() = runTest {
        val sdk = startTracker()
        val navigation = CheckInShopPickerNavigation({ true }, {}, {})
        navigation.close(back = false)
        CheckInFlowHandoff.cancelFromBack()
        navigation.close(back = true)
        runCurrent()
        assertNull(CheckInFlowHandoff.tracker)
        val event = sdk.events.single { it.first == "check_in_cancelled" }.second
        assertEquals("close", event["reason"])
    }

    @Test fun backCancelsOnceWithItsLegacyReason() = runTest {
        val sdk = startTracker()
        val navigation = CheckInShopPickerNavigation({ true }, {}, {})
        navigation.close(back = true)
        CheckInFlowHandoff.cancelFromBack()
        runCurrent()
        assertEquals("back", sdk.events.single { it.first == "check_in_cancelled" }.second["reason"])
    }

    @Test fun declinedConsentNeverEmitsPickerCancellation() = runTest {
        val sdk = startTracker(AnalyticsConsent.Declined)
        CheckInShopPickerNavigation({ true }, {}, {}).close(back = false)
        runCurrent()
        assertTrue(sdk.events.isEmpty())
    }
}

private suspend fun TestScope.startTracker(consent: AnalyticsConsent = AnalyticsConsent.Accepted): RecordingTransport {
    val sdk = RecordingTransport()
    val service = analytics(MemoryConsent(consent), sdk)
    service.setForeground(true)
    service.restoreConsent()
    service.syncIdentity("fixture-owner")
    Analytics.install(service)
    CheckInFlowHandoff.tracker = CheckInTracker(CheckInKind.Visit, CheckInSource.Global)
    CheckInFlowHandoff.tracker?.step(CheckInStep.Shop)
    return sdk
}
