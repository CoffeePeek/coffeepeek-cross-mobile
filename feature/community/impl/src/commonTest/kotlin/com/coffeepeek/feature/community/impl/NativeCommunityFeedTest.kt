package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.api.CommunityCardActions
import com.coffeepeek.feature.community.impl.timeline.*
import com.coffeepeek.feature.community.impl.ui.*
import kotlin.test.*

class NativeCommunityFeedTest {
    @Test fun refreshIsNotOfferedWhileMutationIsInFlight() {
        val state = CommunityState(isLoggedIn = true, helpfulIds = setOf("pending"))
        val actions = nativeFeedCommands(state, true, { error("Unexpected request") }, {}, {}, {})
        assertFalse("refresh" in actions)
        assertFalse("loadMore" in actions)
    }

    @Test fun overlayRetainsCardKeysButBlocksActionsUntilDismissed() {
        val calls = mutableListOf<String>()
        val original = mapOf("home:1" to CommunityCardActions(mapOf("edit" to { calls += "edit" })) { calls += "photo:$it" })
        val covered = nativeFeedCardActions(original, false)
        assertEquals(original.keys, covered.keys)
        assertTrue(covered.getValue("home:1").actions.isEmpty())
        covered.getValue("home:1").onPhoto(0)
        assertTrue(calls.isEmpty())
        val visible = nativeFeedCardActions(original, true).getValue("home:1")
        visible.actions.getValue("edit")()
        visible.onPhoto(2)
        assertEquals(listOf("edit", "photo:2"), calls)
    }

    @Test fun pagingIsBlockedForGuestsOverlaysFailuresAndConflictingWork() {
        val ready = CommunityState(isLoggedIn = true, hasMore = true)
        assertTrue(nativeFeedCanLoadMore(ready, true))
        assertFalse(nativeFeedCanLoadMore(ready, false))
        listOf(ready.copy(isLoggedIn = false), ready.copy(isLoggedIn = null), ready.copy(isLoading = true),
            ready.copy(isLoadingMore = true), ready.copy(isSaving = true), ready.copy(hasMore = false),
            ready.copy(error = TimelineError.LoadFailed)).forEach { assertFalse(nativeFeedCanLoadMore(it, true)) }
    }

    @Test fun visibleConfirmedSaveDoesNotDisappearBehindInitialLoading() {
        assertTrue(nativeFeedLoading(CommunityState(isLoggedIn = null)))
        val loading = CommunityState(isLoggedIn = true, isLoading = true)
        assertTrue(nativeFeedLoading(loading))
        assertFalse(nativeFeedLoading(loading.copy(confirmedSaves = listOf("saved"))))
        assertFalse(nativeFeedLoading(loading.copy(items = listOf(previewPost()))))
    }

    @Test fun nativeRefreshPreservesGestureAnalyticsAndSingleRefreshCallback() {
        val sent = mutableListOf<CommunityAction>()
        var refreshed = 0
        val actions = nativeFeedCommands(CommunityState(isLoggedIn = true), true, sent::add, {}, {}, { refreshed++ })
        actions.getValue("refresh")()
        assertEquals(listOf<CommunityAction>(CommunityAction.Refresh(FeedTrigger.Gesture)), sent)
        assertEquals(1, refreshed)
    }

    @Test fun hiddenConfirmedSaveAndNonInteractiveFeedOfferNoNavigationCallbacks() {
        val state = CommunityState(isLoggedIn = true, confirmedSaves = listOf("saved"), timeline = CommunityTimeline.Mine)
        val actions = nativeFeedCommands(state, true, {}, {}, { error("Hidden save opened") }, {})
        assertFalse("openConfirmed:saved" in actions)
        assertTrue(nativeFeedCommands(state, false, {}, {}, {}, {}).isEmpty())
    }
}
