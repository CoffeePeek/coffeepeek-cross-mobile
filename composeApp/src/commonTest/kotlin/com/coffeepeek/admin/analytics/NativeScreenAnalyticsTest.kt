@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

class NativeScreenAnalyticsTest {
    private val visible = MutableStateFlow(true)
    private val measured = MutableStateFlow(true)
    private val sdk = RecordingTransport()
    private suspend fun TestScope.source(): Pair<AnalyticsService, NativeScreenAnalytics> {
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity("fixture")
        service.screenShown("roaster_detail", correlationId("native-test"))
        return service to NativeScreenAnalytics(backgroundScope, "roaster_detail", visible, service)
    }
    private fun exposures() = sdk.events.count { it.first == "feature_exposed" }

    @Test fun temporaryOcclusionRestartsTheUninterruptedExposureWindow() = runTest {
        val (service, source) = source()
        source.feature("roaster_photos", "roaster_detail", measured)
        runCurrent()
        advanceTimeBy(400)
        service.overlay("photo", true)
        runCurrent()
        advanceTimeBy(200)
        assertEquals(0, exposures())
        service.overlay("photo", false)
        runCurrent()
        advanceTimeBy(499)
        assertEquals(0, exposures())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, exposures())
    }

    @Test fun hiddenOrBackgroundedControllerDoesNotReportAnExposure() = runTest {
        val (service, source) = source()
        source.feature("roaster_contacts", "roaster_detail", measured)
        runCurrent()
        visible.value = false
        runCurrent()
        advanceTimeBy(600)
        assertEquals(0, exposures())
        visible.value = true
        service.setForeground(false)
        runCurrent()
        advanceTimeBy(600)
        assertEquals(0, exposures())
    }

    @Test fun returningToTheSameVisitDoesNotDuplicateFeatureExposure() = runTest {
        val (_, source) = source()
        source.feature("roaster_shops", "roaster_detail", measured)
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        visible.value = false
        runCurrent()
        visible.value = true
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, exposures())
    }

    @Test fun entityViewWaitsForLoadedContentAndIsOncePerVisit() = runTest {
        val (_, source) = source()
        measured.value = false
        source.entity(Entity.Roaster, measured)
        runCurrent()
        assertTrue(sdk.events.none { it.first == "entity_detail_viewed" })
        measured.value = true
        runCurrent()
        visible.value = false
        runCurrent()
        visible.value = true
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "entity_detail_viewed" })
    }

    @Test fun nativeFeedViewWaitsForForegroundAndFirstPageAndDoesNotRepeatOnRefresh() = runTest {
        val (service, source) = source()
        service.screenShown("feed", correlationId("feed-test"))
        service.setForeground(false)
        val page = MutableStateFlow<Pair<Int?, Boolean>>(null to true)
        source.feedViewed(page)
        page.value = 0 to true
        runCurrent()
        assertTrue(sdk.events.none { it.first == "feed_viewed" })
        service.setForeground(true)
        runCurrent()
        page.value = 4 to true
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "feed_viewed" })
    }
}
