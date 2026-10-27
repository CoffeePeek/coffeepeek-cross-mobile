package com.coffeepeek.feature.roaster.api

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

// shortcut: explicit arguments avoid KT-87326; restore defaults after upgrading to a compiler with the fix.

/**
 * Discovery hosts the roaster carousel and the roaster search overlay with one shared state (tags, favorites,
 * loaded details). Call both in the same ViewModel-store owner; the caller owns the criteria and navigation.
 */
interface RoasterDiscoveryEntry {
    /** Whether the filtered carousel has results; null while loading or after a failed load. */
    @Composable
    fun hasResults(query: String, selectedRoasters: Set<String>, favoritesOnly: Boolean): Boolean?

    @Composable
    fun Section(
        query: String,
        selectedRoasters: Set<String>,
        favoritesOnly: Boolean,
        onOpenRoaster: (slug: String) -> Unit,
        modifier: Modifier,
    )

    /** The full list shown while searching roasters. [header] is the app's search header. */
    @Composable
    fun Search(
        query: String,
        selectedRoasters: Set<String>,
        favoritesOnly: Boolean,
        onOpenRoaster: (slug: String) -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
        header: @Composable ColumnScope.() -> Unit,
    )
}

/** Caller owns destination lifetime, root navigation and platform browser actions. */
interface RoasterDetailEntry {
    @Composable
    fun Content(
        slug: String,
        onBack: () -> Unit,
        onOpenShop: (slug: String) -> Unit,
        onOpenLink: (url: String) -> Unit,
        modifier: Modifier,
    )
}
