package com.coffeepeek.feature.roaster.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

// shortcut: explicit arguments avoid KT-87326; restore defaults after upgrading to a compiler with the fix.

/**
 * Discovery hosts the roaster carousel and the roaster search overlay with one shared state (tags, favorites,
 * loaded details). Call both in the same ViewModel-store owner; the caller owns the criteria and navigation.
 */
interface RoasterDiscoveryEntry {
    @Composable
    fun Section(
        query: String,
        selectedRoasters: Set<String>,
        favoritesOnly: Boolean,
        onOpenRoaster: (slug: String) -> Unit,
        onAddRoaster: () -> Unit,
        modifier: Modifier,
    )

    /** The feature owns its header; Discovery retains the query and overlay navigation. */
    @Composable
    fun Search(
        query: String,
        selectedRoasters: Set<String>,
        favoritesOnly: Boolean,
        onOpenRoaster: (slug: String) -> Unit,
        onAddRoaster: () -> Unit,
        onQueryChange: (String) -> Unit,
        onCancel: () -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
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
