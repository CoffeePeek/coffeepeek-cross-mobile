package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.screen.checkins.CheckInInlineAction
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.screen.main.CheckInTabContent
import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterContext
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterSlots
import com.coffeepeek.feature.discovery.api.DiscoveryTabEntry
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.impl.createDiscoveryTabEntry
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

/** Android routes the discovery tab to the migrated entry; navigation, the check-in button and the sign-in stay app-owned. */
internal class AndroidDiscoveryScreenRenderer(private val entry: DiscoveryTabEntry) : DiscoveryScreenRenderer {
    @Composable
    override fun Tab(onMapExpandedChange: (Boolean) -> Unit, reselections: Flow<Unit>) {
        val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
        val clearance = LocalFloatingNavClearance.current
        CheckInTabContent(showCreateButton = false) { onCreateCheckIn ->
            entry.Content(
                onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
                onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
                onAddShop = { Navigator.navigate(Navigator.Screen.AddShop) },
                onMapExpandedChange = onMapExpandedChange,
                bottomClearance = clearance,
                focus = pendingMapFocus?.let { MapFocus(it.shopId, it.title, Coordinates(it.latitude, it.longitude)) },
                onFocusConsumed = Navigator::consumeMapFocus,
                checkInAction = { CheckInInlineAction(onCreateCheckIn) },
                reselections = reselections,
            )
        }
    }
}

internal fun androidDiscoveryScreenRenderer(
    client: HttpClient,
    shops: ShopRepository,
    sessions: SessionReader,
    favorites: FavoritesRepository,
    cityPreference: CityPreference,
    roasters: RoasterScreenRenderer,
): DiscoveryScreenRenderer {
    val favoriteAdapter = AppDiscoveryFavorites(favorites)
    val repository = createDiscoveryRepository(client, favoriteAdapter::favoriteIds, roasterEntries(shops))
    val slots = DiscoveryRoasterSlots(
        preview = { context -> roasters.DiscoverySection(context.query, context.selectedRoasterIds, context.favoritesOnly) },
        search = { context: DiscoveryRoasterContext, onQueryChange, onCancel ->
            roasters.DiscoverySearch(onCancel, context.query, onQueryChange, context.selectedRoasterIds, context.favoritesOnly)
        },
        hasResults = { context -> roasters.hasDiscoveryResults(context.query, context.selectedRoasterIds, context.favoritesOnly) },
        onAdd = { Navigator.navigate(Navigator.Screen.AddRoaster) },
    )
    return AndroidDiscoveryScreenRenderer(
        createDiscoveryTabEntry(
            repository = repository,
            favorites = favoriteAdapter,
            city = PreferenceDiscoveryCity(cityPreference),
            location = AppDiscoveryLocation(),
            account = SessionDiscoveryAccount(sessions),
            shopDetails = ShopDiscoveryDetails(shops),
            analytics = AppDiscoveryAnalytics(),
            host = discoveryHost(),
            roasters = slots,
        ),
    )
}
