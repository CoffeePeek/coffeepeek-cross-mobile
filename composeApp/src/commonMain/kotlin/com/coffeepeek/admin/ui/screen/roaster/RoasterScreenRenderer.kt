package com.coffeepeek.admin.ui.screen.roaster

import androidx.compose.runtime.Composable

/**
 * Platform selection only. Shared/iOS keep the legacy roaster UI; Android composes the migrated entries.
 * Discovery's carousel and its search overlay share one list state through the same renderer instance.
 */
interface RoasterScreenRenderer {
    @Composable
    fun DiscoverySection(query: String, selectedRoasterIds: Set<String>, favoritesOnly: Boolean)

    @Composable
    fun DiscoverySearch(
        onCancel: () -> Unit,
        query: String,
        onQueryChange: (String) -> Unit,
        selectedRoasterIds: Set<String>,
        favoritesOnly: Boolean,
    )

    @Composable
    fun Detail(roasterId: String)
}

internal object LegacyRoasterScreenRenderer : RoasterScreenRenderer {
    @Composable
    override fun DiscoverySection(query: String, selectedRoasterIds: Set<String>, favoritesOnly: Boolean) =
        RoasterPreview(query, selectedRoasterIds, favoritesOnly)

    @Composable
    override fun DiscoverySearch(
        onCancel: () -> Unit,
        query: String,
        onQueryChange: (String) -> Unit,
        selectedRoasterIds: Set<String>,
        favoritesOnly: Boolean,
    ) = RoasterListScreen(onCancel, query, onQueryChange, selectedRoasterIds, favoritesOnly)

    @Composable
    override fun Detail(roasterId: String) = RoasterDetailScreen(roasterId)
}
