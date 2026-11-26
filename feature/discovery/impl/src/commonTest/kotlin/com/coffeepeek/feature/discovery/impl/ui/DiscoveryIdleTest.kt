package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.search.SearchError
import com.coffeepeek.feature.discovery.impl.search.SearchState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryIdleTest {
    @Test fun pickerAndCoveredOrMovingNativeSurfacesNeverRequestAppReview() {
        val idle = SearchState(isLoading = false)
        assertTrue(nativeDiscoveryCanPromptReview(idle, false, true, false, false, false))
        assertFalse(nativeDiscoveryCanPromptReview(idle, true, true, false, false, false))
        assertFalse(nativeDiscoveryCanPromptReview(idle, false, false, false, false, false))
        assertFalse(nativeDiscoveryCanPromptReview(idle, false, true, true, false, false))
        assertFalse(nativeDiscoveryCanPromptReview(idle, false, true, false, true, false))
        assertFalse(nativeDiscoveryCanPromptReview(idle, false, true, false, false, true))
    }

    @Test fun refreshAndLocationOrSearchWorkRetainTheExistingDiscoveryIdleGuards() {
        val idle = SearchState(isLoading = false)
        listOf(idle.copy(isLoading = true), idle.copy(isRefreshing = true),
            idle.copy(error = SearchError(null)), idle.copy(showFilters = true),
            idle.copy(nearbyRequested = true), idle.copy(locationUnavailable = true),
            idle.copy(locationSettingsNeeded = true), idle.copy(query = "coffee"),
            idle.copy(searchActive = true)).forEach { state ->
            assertFalse(state.isIdleDiscoveryContent())
            assertFalse(nativeDiscoveryCanPromptReview(state, false, true, false, false, false))
        }
    }

    @Test fun nativeDraggingAndDecelerationKeepReviewPendingUntilTheListSettles() = runTest {
        val scrolling = DiscoveryNativeScrolling(backgroundScope)
        assertTrue(scrolling.record("scroll-start"))
        advanceTimeBy(1000)
        assertTrue(scrolling.active)
        scrolling.record("scroll-end")
        runCurrent()
        advanceTimeBy(200)
        scrolling.record("scroll")
        runCurrent()
        advanceTimeBy(200)
        assertTrue(scrolling.active)
        advanceTimeBy(150)
        runCurrent()
        assertFalse(scrolling.active)
    }

    @Test fun lifecycleResetClearsDraggingAndOrdinaryActionsDoNotPretendToScroll() = runTest {
        val scrolling = DiscoveryNativeScrolling(backgroundScope)
        scrolling.record("scroll-start")
        scrolling.reset()
        assertFalse(scrolling.active)
        assertFalse(scrolling.record("favorite"))
        scrolling.record("scroll")
        runCurrent()
        advanceTimeBy(350)
        runCurrent()
        assertFalse(scrolling.active)
    }
}
