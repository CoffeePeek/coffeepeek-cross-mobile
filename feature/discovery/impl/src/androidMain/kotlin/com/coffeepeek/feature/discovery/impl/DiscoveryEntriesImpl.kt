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
import com.coffeepeek.feature.discovery.api.DiscoverySearchEntry
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import com.coffeepeek.feature.discovery.impl.ui.SearchCallbacks
import com.coffeepeek.feature.discovery.impl.ui.SearchScreen

/** The shop search. The ViewModel lives in the destination's store (one search per tab), so the query and the filters survive a shop opened on top. */
fun createDiscoverySearchEntry(
    repository: DiscoveryRepository,
    favorites: DiscoveryFavorites,
    city: DiscoveryCity,
    location: DiscoveryLocation,
    account: DiscoveryAccount,
    analytics: DiscoveryAnalytics,
    host: DiscoveryHost,
): DiscoverySearchEntry = object : DiscoverySearchEntry {
    @Composable
    override fun Content(
        onOpenShop: (String) -> Unit,
        onSignIn: () -> Unit,
        onAddShop: () -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
        discovery: @Composable () -> Unit,
        textResults: (@Composable () -> Unit)?,
    ) {
        SearchScreen(
            viewModel(key = "discovery-search") { SearchViewModel(repository, favorites, city, location, account, analytics) },
            host, SearchCallbacks(onOpenShop, onSignIn, onAddShop), bottomClearance, discovery, textResults, modifier,
        )
    }
}
