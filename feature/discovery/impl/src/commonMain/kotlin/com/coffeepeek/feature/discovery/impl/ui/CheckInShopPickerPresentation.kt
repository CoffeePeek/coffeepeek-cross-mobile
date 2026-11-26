package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.domain.rules.searchText
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState
import com.coffeepeek.feature.discovery.impl.search.matchesPickerQuery

/** The scroll position follows accepted results, while typed criteria can still be pending. */
internal data class CheckInShopPickerResults(val query: String?, val nearby: Boolean, val pending: Boolean)

internal fun SearchState.checkInShopPickerResults(): CheckInShopPickerResults {
    val applied = pickerResultCriteria
    return CheckInShopPickerResults(searchText(applied?.text.orEmpty()), applied?.filters?.nearbyOnly == true,
        applied?.matchesPickerQuery(criteria) != true && error == null)
}

/** Returning from the map must keep an already active Nearby filter. */
internal fun pickerNearbyAction(showMap: Boolean, nearbyOnly: Boolean): SearchAction? =
    SearchAction.ToggleNearby.takeIf { !showMap || !nearbyOnly }
