package com.coffeepeek.feature.discovery.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryFavorites
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterSlots
import com.coffeepeek.feature.discovery.api.DiscoveryShopDetails
import com.coffeepeek.feature.discovery.api.DiscoveryTabEntry
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryTab
import com.coffeepeek.feature.discovery.impl.ui.TabCallbacks

/**
 * The whole discovery tab (search, mini map that expands, shops, roaster sections). The two ViewModels live in the destination's store, so the query,
 * the filters, the camera and the selection survive a shop opened on top.
 */
fun createDiscoveryTabEntry(
    repository: DiscoveryRepository,
    favorites: DiscoveryFavorites,
    city: DiscoveryCity,
    location: DiscoveryLocation,
    account: DiscoveryAccount,
    shopDetails: DiscoveryShopDetails,
    analytics: DiscoveryAnalytics,
    host: DiscoveryHost,
    roasters: DiscoveryRoasterSlots,
): DiscoveryTabEntry = object : DiscoveryTabEntry {
    @Composable
    override fun Content(
        onOpenShop: (String) -> Unit,
        onSignIn: () -> Unit,
        onAddShop: () -> Unit,
        onMapExpandedChange: (Boolean) -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
        focus: MapFocus?,
        onFocusConsumed: () -> Unit,
        checkInAction: @Composable () -> Unit,
        reselections: kotlinx.coroutines.flow.Flow<Unit>,
    ) {
        DiscoveryTab(
            viewModel(key = "discovery-search") { SearchViewModel(repository, favorites, city, location, account, analytics) },
            viewModel(key = "discovery-map") { MapViewModel(repository, city, location, shopDetails, analytics, account, favorites) },
            host, roasters, analytics,
            TabCallbacks(onOpenShop, onSignIn, onAddShop, onMapExpandedChange, onFocusConsumed, checkInAction, reselections),
            bottomClearance, focus, modifier,
        )
    }
}
