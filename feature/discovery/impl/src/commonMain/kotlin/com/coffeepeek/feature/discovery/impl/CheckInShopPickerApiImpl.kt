package com.coffeepeek.feature.discovery.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchPurpose
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import com.coffeepeek.feature.discovery.impl.ui.CheckInShopPickerScreen

fun createCheckInShopPickerEntry(
    repository: DiscoveryRepository, favorites: DiscoveryFavorites, city: DiscoveryCity,
    location: DiscoveryLocation, account: DiscoveryAccount, details: DiscoveryShopDetails,
    analytics: DiscoveryAnalytics, host: DiscoveryHost,
): CheckInShopPickerEntry = object : CheckInShopPickerEntry {
    @Composable
    override fun Content(onBack: () -> Unit, onClose: () -> Unit, onSelectShop: (String) -> Unit,
        onSignIn: () -> Unit, modifier: Modifier) {
        val search = viewModel(key = "check-in-shop-search") {
            SearchViewModel(repository, favorites, city, location, account, analytics, SearchPurpose.CheckInPicker)
        }
        val map = viewModel(key = "check-in-shop-map") { MapViewModel(repository, city, location, details, analytics) }
        CheckInShopPickerScreen(search, map, host, onBack, onClose, onSelectShop, onSignIn, modifier)
    }
}
