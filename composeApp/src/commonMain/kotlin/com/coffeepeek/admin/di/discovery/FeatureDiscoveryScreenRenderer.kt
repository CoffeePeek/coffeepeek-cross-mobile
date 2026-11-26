package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.admin.di.shop.AppShopCheckInDraftStore
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/** Both platforms use the migrated search; navigation and native map remain app-owned. */
internal class FeatureDiscoveryScreenRenderer(private val entry: DiscoveryTabEntry, private val sessions: AppDiscoverySessions) :
    DiscoveryScreenRenderer, NativeDiscoverySessionFactory {
    override fun createNativeSession() = sessions.nativeSession()
    @Composable
    override fun Tab(onMapExpandedChange: (Boolean) -> Unit, reselections: Flow<Unit>) {
        val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
        val clearance = LocalFloatingNavClearance.current
        val resumeCheckIn = checkInResumeAction()
        entry.Content(
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
            onAddShop = { Navigator.navigate(Navigator.Screen.AddShop) },
            onMapExpandedChange = onMapExpandedChange,
            modifier = Modifier,
            bottomClearance = clearance,
            focus = pendingMapFocus?.let { MapFocus(it.shopId, it.title, Coordinates(it.latitude, it.longitude)) },
            onFocusConsumed = Navigator::consumeMapFocus,
            reselections = reselections,
            onContinueCheckIn = resumeCheckIn,
        )
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
private fun checkInResumeAction(): (() -> Unit)? {
    val sessions = koinInject<SessionReader>()
    val drafts = koinInject<AppShopCheckInDraftStore>()
    val destination by remember(sessions, drafts) {
        observeCheckInResumeDestination(sessions, drafts)
    }.collectAsStateWithLifecycle(initialValue = null)
    val current = activeShopResumeDestination(destination, sessions) ?: return null
    return { resumeShopCheckIn(current, sessions) }
}

internal fun featureDiscoveryScreenRenderer(
    client: HttpClient,
    shops: PublicShopPreviews,
    catalog: PublicRoasterCatalog,
    sessions: SessionReader,
    favorites: FavoritesRepository,
    city: PreferenceDiscoveryCity,
    roasters: RoasterScreenRenderer,
    location: AppDiscoveryLocation,
    favoriteCollections: FavoriteCollectionsPrompt?,
): DiscoveryScreenRenderer {
    val favoriteAdapter = AppDiscoveryFavorites(favorites, favoriteCollections)
    val repository = createDiscoveryRepository(client, favoriteAdapter::favoriteIds, roasterEntries(catalog))
    val assembly = AppDiscoverySessions(repository, favoriteAdapter, city, location, SessionDiscoveryAccount(sessions), ShopDiscoveryDetails(shops))
    return FeatureDiscoveryScreenRenderer(assembly.entry(discoveryHost(), discoveryRoasterSlots(roasters)), assembly)
}

private fun discoveryRoasterSlots(roasters: RoasterScreenRenderer) = DiscoveryRoasterSlots(
    preview = { context -> roasters.DiscoverySection(context.query, context.selectedRoasterIds, context.favoritesOnly) },
    search = { context: DiscoveryRoasterContext, onQueryChange, onCancel ->
        roasters.DiscoverySearch(onCancel, context.query, onQueryChange, context.selectedRoasterIds, context.favoritesOnly)
    },
    hasResults = { context -> roasters.hasDiscoveryResults(context.query, context.selectedRoasterIds, context.favoritesOnly) },
)
