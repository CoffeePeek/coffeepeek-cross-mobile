package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.screen.main.CheckInTabContent
import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterContext
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterSlots
import com.coffeepeek.feature.discovery.api.DiscoveryTabEntry
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.impl.createDiscoveryTabEntry
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope

/** Both platforms use the migrated search; navigation, native map and global creation remain app-owned. */
internal class FeatureDiscoveryScreenRenderer(private val entry: DiscoveryTabEntry) : DiscoveryScreenRenderer {
    @Composable
    override fun Tab(onMapExpandedChange: (Boolean) -> Unit) {
        val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
        var isMapExpanded by remember { mutableStateOf(false) }
        val clearance = LocalFloatingNavClearance.current
        CheckInTabContent(showCreateButton = !isMapExpanded) {
            entry.Content(
                onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
                onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
                onAddShop = { Navigator.navigate(Navigator.Screen.AddShop) },
                onMapExpandedChange = { isMapExpanded = it; onMapExpandedChange(it) },
                modifier = Modifier,
                bottomClearance = clearance,
                focus = pendingMapFocus?.let { MapFocus(it.shopId, it.title, Coordinates(it.latitude, it.longitude)) },
                onFocusConsumed = Navigator::consumeMapFocus,
            )
        }
    }
}

internal fun featureDiscoveryScreenRenderer(
    client: HttpClient,
    shops: PublicShopPreviews,
    catalog: PublicRoasterCatalog,
    sessions: SessionReader,
    favorites: FavoritesRepository,
    cityPreference: CityPreference,
    roasters: RoasterScreenRenderer,
    scope: CoroutineScope,
): DiscoveryScreenRenderer {
    val favoriteAdapter = AppDiscoveryFavorites(favorites)
    val repository = createDiscoveryRepository(client, favoriteAdapter::favoriteIds, roasterEntries(catalog))
    return FeatureDiscoveryScreenRenderer(
        createDiscoveryTabEntry(
            repository = repository,
            favorites = favoriteAdapter,
            city = PreferenceDiscoveryCity(cityPreference),
            location = AppDiscoveryLocation(scope),
            account = SessionDiscoveryAccount(sessions),
            shopDetails = ShopDiscoveryDetails(shops),
            analytics = AppDiscoveryAnalytics(),
            host = discoveryHost(),
            roasters = discoveryRoasterSlots(roasters),
        ),
    )
}

private fun discoveryRoasterSlots(roasters: RoasterScreenRenderer) = DiscoveryRoasterSlots(
    preview = { context -> roasters.DiscoverySection(context.query, context.selectedRoasterIds, context.favoritesOnly) },
    search = { context: DiscoveryRoasterContext, onQueryChange, onCancel ->
        roasters.DiscoverySearch(onCancel, context.query, onQueryChange, context.selectedRoasterIds, context.favoritesOnly)
    },
)
