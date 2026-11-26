package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchError
import com.coffeepeek.feature.discovery.impl.search.SearchState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckInShopPickerPresentationTest {
    private val accepted = DiscoveryCriteria("coffee", DiscoveryFilters(), null)
    private val state = SearchState(query = "coffee", isLoading = false, pickerResultCriteria = accepted)

    @Test fun typingDuringDebounceKeepsTheAcceptedScrollCriteria() {
        val before = state.checkInShopPickerResults()
        val typing = state.copy(query = "tea").checkInShopPickerResults()
        assertEquals(before.query, typing.query)
        assertEquals(before.nearby, typing.nearby)
        assertTrue(typing.pending)
    }

    @Test fun onlyAcceptedResultsChangeTheScrollCriteria() {
        val typing = state.copy(query = "tea")
        val received = typing.copy(pickerResultCriteria = accepted.copy(text = "tea"))
        assertNotEquals(typing.checkInShopPickerResults().query, received.checkInShopPickerResults().query)
        assertFalse(received.checkInShopPickerResults().pending)
        assertEquals(received.checkInShopPickerResults(), received.copy(isLoadingMore = true).checkInShopPickerResults())
    }

    @Test fun surroundingWhitespaceDoesNotCreateAFalsePendingSearchOrScrollReset() {
        val spaced = state.copy(query = " coffee ", pickerResultCriteria = accepted.copy(text = " coffee "))
        assertEquals(state.checkInShopPickerResults(), spaced.checkInShopPickerResults())
        assertFalse(state.copy(query = " coffee ").checkInShopPickerResults().pending)
    }

    @Test fun emptyInitialResultsRemainPendingUntilTheFirstResponse() {
        assertTrue(SearchState(isLoading = false).checkInShopPickerResults().pending)
        val initial = state.copy(query = "", pickerResultCriteria = accepted.copy(text = ""))
        assertFalse(initial.checkInShopPickerResults().pending)
        assertNull(initial.checkInShopPickerResults().query)
    }

    @Test fun aFailedSearchShowsRecoveryInsteadOfAnEndlessPendingIndicator() {
        val failed = state.copy(query = "tea", error = SearchError(null)).checkInShopPickerResults()
        assertFalse(failed.pending)
        assertEquals("coffee", failed.query)
    }

    @Test fun nearbyKeepsCityScrollCriteriaUntilItsResponseIsAccepted() {
        val nearby = state.copy(filters = DiscoveryFilters(nearbyOnly = true), userLocation = Coordinates(53.9, 27.56))
        assertFalse(nearby.checkInShopPickerResults().nearby)
        assertTrue(nearby.checkInShopPickerResults().pending)
        val received = nearby.copy(pickerResultCriteria = nearby.criteria).checkInShopPickerResults()
        assertTrue(received.nearby)
        assertFalse(received.pending)
    }

    @Test fun newerNearbyPositionWaitsForItsResponseWithoutResettingTheScrollKey() {
        val nearby = state.copy(filters = DiscoveryFilters(nearbyOnly = true), userLocation = Coordinates(53.9, 27.56))
        val received = nearby.copy(pickerResultCriteria = nearby.criteria)
        val moved = received.copy(userLocation = Coordinates(53.91, 27.57)).checkInShopPickerResults()
        assertTrue(moved.pending)
        assertEquals(received.checkInShopPickerResults().query, moved.query)
        assertEquals(received.checkInShopPickerResults().nearby, moved.nearby)
    }

    @Test fun returningFromTheMapKeepsNearbyAndListTapsStillToggleIt() {
        assertNull(pickerNearbyAction(showMap = true, nearbyOnly = true))
        assertEquals(SearchAction.ToggleNearby, pickerNearbyAction(showMap = true, nearbyOnly = false))
        assertEquals(SearchAction.ToggleNearby, pickerNearbyAction(showMap = false, nearbyOnly = true))
        assertEquals(SearchAction.ToggleNearby, pickerNearbyAction(showMap = false, nearbyOnly = false))
    }
}
