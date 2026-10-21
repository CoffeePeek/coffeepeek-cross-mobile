package com.coffeepeek.feature.roaster.api

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
        modifier: Modifier = Modifier,
    )

    /** The full list shown while searching roasters. [header] is the app's search header. */
    @Composable
    fun Search(
        query: String,
        selectedRoasters: Set<String>,
        favoritesOnly: Boolean,
        onOpenRoaster: (slug: String) -> Unit,
        modifier: Modifier = Modifier,
        bottomClearance: Dp = 0.dp,
        header: @Composable ColumnScope.() -> Unit = {},
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
        modifier: Modifier = Modifier,
    )
}
