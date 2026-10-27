package com.coffeepeek.admin.di.roaster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.feature.roaster.api.RoasterDetailEntry
import com.coffeepeek.feature.roaster.api.RoasterDiscoveryEntry
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.createRoasterDetailEntry
import com.coffeepeek.feature.roaster.impl.createRoasterDiscoveryEntry

/** Root routes platform actions; search and empty/error content belong to the roaster feature. */
internal class FeatureRoasterScreenRenderer(
    private val discovery: RoasterDiscoveryEntry,
    private val detail: RoasterDetailEntry,
) : RoasterScreenRenderer {
    @Composable
    override fun hasDiscoveryResults(query: String, selectedRoasterIds: Set<String>, favoritesOnly: Boolean): Boolean? =
        discovery.hasResults(query, selectedRoasterIds, favoritesOnly)

    @Composable
    override fun DiscoverySection(query: String, selectedRoasterIds: Set<String>, favoritesOnly: Boolean) {
        discovery.Section(query, selectedRoasterIds, favoritesOnly, onOpenRoaster = ::openRoaster, onAddRoaster = ::addRoaster, modifier = Modifier)
    }

    @Composable
    override fun DiscoverySearch(onCancel: () -> Unit, query: String, onQueryChange: (String) -> Unit,
        selectedRoasterIds: Set<String>, favoritesOnly: Boolean) {
        discovery.Search(query, selectedRoasterIds, favoritesOnly, onOpenRoaster = ::openRoaster,
            onAddRoaster = ::addRoaster, onQueryChange = onQueryChange, onCancel = onCancel,
            modifier = Modifier, bottomClearance = LocalFloatingNavClearance.current)
    }

    @Composable
    override fun Detail(roasterId: String) {
        detail.Content(roasterId, onBack = Navigator::popBack,
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenLink = OpenInBrowser::openInBrowser, modifier = Modifier)
    }

    private fun addRoaster() = Navigator.navigate(Navigator.Screen.AddRoaster)

    private fun openRoaster(slug: String) = Navigator.navigate(Navigator.Screen.RoasterDetail(slug))
}

internal fun roasterScreenRenderer(
    repository: RoasterRepository,
    favorites: RoasterFavoritesPort,
    shopPhotos: RoasterShopPhotos,
): FeatureRoasterScreenRenderer {
    val feedback = { platformHaptic(HapticEvent.Selection) }
    return FeatureRoasterScreenRenderer(
        createRoasterDiscoveryEntry(repository, favorites, RoasterAnalyticsAdapter, RoasterScreenAnalyticsAdapter,
            onFavoriteFeedback = feedback),
        createRoasterDetailEntry(repository, favorites, shopPhotos, RoasterAnalyticsAdapter, RoasterScreenAnalyticsAdapter,
            onFavoriteFeedback = feedback),
    )
}
