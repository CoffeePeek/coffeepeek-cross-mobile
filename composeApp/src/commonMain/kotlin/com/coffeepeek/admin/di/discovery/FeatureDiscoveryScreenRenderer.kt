package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.admin.di.shop.AppShopCheckInDraftStore
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateButton
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/** Both platforms use the migrated search; navigation, native map and global creation remain app-owned. */
internal class FeatureDiscoveryScreenRenderer(private val entry: DiscoveryTabEntry) : DiscoveryScreenRenderer {
    @Composable
    override fun Tab(onMapExpandedChange: (Boolean) -> Unit, reselections: Flow<Unit>) {
        val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
        val clearance = LocalFloatingNavClearance.current
        val resumeCheckIn = checkInResumeAction()
        CheckInTabContent(showCreateButton = false) { onCreateCheckIn ->
            entry.Content(
                onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
                onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
                onAddShop = { Navigator.navigate(Navigator.Screen.AddShop) },
                onMapExpandedChange = onMapExpandedChange,
                modifier = Modifier,
                bottomClearance = clearance,
                focus = pendingMapFocus?.let { MapFocus(it.shopId, it.title, Coordinates(it.latitude, it.longitude)) },
                onFocusConsumed = Navigator::consumeMapFocus,
                checkInAction = { CheckInCreateButton(onClick = onCreateCheckIn) },
                reselections = reselections,
                onContinueCheckIn = resumeCheckIn,
            )
        }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
private fun checkInResumeAction(): (() -> Unit)? {
    val sessions = koinInject<SessionReader>()
    val drafts = koinInject<AppShopCheckInDraftStore>()
    val destination by remember(sessions, drafts) {
        sessions.observeSession()
            .map { it?.takeIf(sessions::isActiveSession)?.userId }
            .distinctUntilChanged()
            .flatMapLatest { owner ->
                if (owner == null) flowOf(null)
                else drafts.observeLatestShop(owner).map { slug -> slug?.let { owner to it } }.catch { emit(null) }
            }
    }.collectAsStateWithLifecycle(initialValue = null)
    val (owner, slug) = destination ?: return null
    val session = sessions.peekSession()
    if (session?.userId != owner || !sessions.isActiveSession(session)) return null
    return {
        val current = sessions.peekSession()
        if (current?.userId == owner && sessions.isActiveSession(current)) {
            Navigator.navigate(Navigator.Screen.ShopDetail(slug, openCheckIn = true))
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
    favoriteCollections: FavoriteCollectionsPrompt?,
): DiscoveryScreenRenderer {
    val favoriteAdapter = AppDiscoveryFavorites(favorites, favoriteCollections)
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
    hasResults = { context -> roasters.hasDiscoveryResults(context.query, context.selectedRoasterIds, context.favoritesOnly) },
)
