package com.coffeepeek.feature.roaster.impl.ui

import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListItem
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoasterSearchResultsTest {
    private val loaded = RoasterListState(isLoading = false, query = "previous query",
        items = listOf(RoasterListItem(summary("a", "Alpha", listOf(tag("light")))),
            RoasterListItem(summary("b", "Beta"))), favoriteKeys = setOf("b"))

    @Test fun usesTheCurrentQueryInsteadOfThePreviouslyRenderedQuery() {
        assertEquals(true, loaded.hasResults(" ALPHA ", emptySet(), false))
        assertEquals(false, loaded.hasResults("cafe", emptySet(), false))
    }

    @Test fun respectsSelectedRoastersFavoritesAndCarouselTags() {
        assertEquals(true, loaded.hasResults("", setOf("a"), false))
        assertEquals(false, loaded.hasResults("Beta", setOf("a"), false))
        assertEquals(false, loaded.hasResults("Alpha", emptySet(), true))
        assertEquals(true, loaded.hasResults("Beta", emptySet(), true))
        assertEquals(false, loaded.copy(selectedTags = setOf("light")).hasResults("Beta", emptySet(), false))
    }

    @Test fun anEmptyLoadedCatalogHasNoResults() {
        assertEquals(false, RoasterListState(isLoading = false).hasResults("", emptySet(), false))
    }

    @Test fun loadingAndFailureAreUnknownRatherThanEmpty() {
        assertNull(loaded.copy(isLoading = true).hasResults("Alpha", emptySet(), false))
        assertNull(loaded.copy(loadFailed = true).hasResults("Alpha", emptySet(), false))
    }
}
