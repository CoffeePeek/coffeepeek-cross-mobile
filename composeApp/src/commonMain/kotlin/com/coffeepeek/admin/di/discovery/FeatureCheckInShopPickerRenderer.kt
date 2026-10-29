package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.di.shop.ShopBrowsingHost
import com.coffeepeek.admin.di.shop.ShopBrowsingHostSession
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.impl.createCheckInShopPickerEntry
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope

internal class FeatureCheckInShopPickerRenderer(
    private val client: HttpClient, private val shops: PublicShopPreviews, private val catalog: PublicRoasterCatalog,
    private val sessions: SessionReader,
    private val favorites: FavoritesRepository, private val city: CityPreference,
) : CheckInShopPickerRenderer {
    @Composable
    override fun Content() {
        ShopBrowsingHost("check-in-shop-picker", sessions) { session -> Picker(session) }
    }

    @Composable
    private fun Picker(session: ShopBrowsingHostSession) {
        val scope = rememberCoroutineScope()
        val entry = remember(session) { entry(session, scope) }
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val navigation = remember(session) {
            CheckInShopPickerNavigation(session.lease::isCurrent, { slug ->
                focus.clearFocus(force = true)
                keyboard?.hide()
                Navigator.popThenNavigate(Navigator.Screen.ShopDetail(slug, forCheckIn = true))
            }, Navigator::popBack)
        }
        DisposableEffect(navigation) { onDispose { navigation.dispose() } }
        LaunchedEffect(session) {
            val tracker = CheckInFlowHandoff.tracker?.takeIf { it.isFresh }
                ?: CheckInTracker(CheckInKind.Visit, CheckInSource.Direct).also { CheckInFlowHandoff.tracker = it }
            tracker.step(CheckInStep.Shop)
        }
        entry.Content({ navigation.close(back = true) }, { navigation.close(back = false) }, navigation::select,
            { if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.Auth) }, Modifier)
    }

    private fun entry(session: ShopBrowsingHostSession, scope: CoroutineScope) =
        AppDiscoveryFavorites(session.lease.favorites(favorites)).let { favoriteAdapter ->
            createCheckInShopPickerEntry(
                createDiscoveryRepository(client, favoriteAdapter::favoriteIds, roasterEntries(catalog)),
                favoriteAdapter, PreferenceDiscoveryCity(city), AppDiscoveryLocation(scope),
                SessionDiscoveryAccount(sessions), ShopDiscoveryDetails(shops), AppDiscoveryAnalytics(), discoveryHost(),
            )
        }
}
