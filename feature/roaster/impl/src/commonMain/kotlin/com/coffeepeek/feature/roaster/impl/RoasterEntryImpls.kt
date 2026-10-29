package com.coffeepeek.feature.roaster.impl

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.roaster.api.RoasterAnalytics
import com.coffeepeek.feature.roaster.api.RoasterDetailEntry
import com.coffeepeek.feature.roaster.api.RoasterDiscoveryEntry
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterImage
import com.coffeepeek.feature.roaster.api.RoasterLoadError
import com.coffeepeek.feature.roaster.api.RoasterPhotoViewer
import com.coffeepeek.feature.roaster.api.RoasterScreenAnalytics
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.ui.RoasterDetailViewModel
import com.coffeepeek.feature.roaster.impl.ui.RoasterListViewModel
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterDetailScreen
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterDiscoverySection
import com.coffeepeek.feature.roaster.impl.ui.compose.roasterDiscoveryState
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterListScreen
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListEvent
import kotlin.uuid.Uuid

/**
 * Construct once and call [RoasterDiscoveryEntry.Section] and [RoasterDiscoveryEntry.Search] in the same
 * ViewModel-store owner: both use one list ViewModel, like the legacy Discovery. The application supplies the image
 * loader, error/empty content, analytics and haptics; no network client or navigation is created here.
 */
fun createRoasterDiscoveryEntry(
    repository: RoasterRepository,
    favorites: RoasterFavoritesPort = RoasterFavoritesPort.None,
    analytics: RoasterAnalytics = RoasterAnalytics.None,
    screenAnalytics: RoasterScreenAnalytics = RoasterScreenAnalytics.None,
    image: RoasterImage,
    loadError: RoasterLoadError,
    emptyContent: @Composable (Modifier) -> Unit,
    onFavoriteFeedback: () -> Unit = {},
): RoasterDiscoveryEntry = RoasterDiscoveryApiImpl(repository, favorites, analytics, screenAnalytics, image, loadError, emptyContent, onFavoriteFeedback)

/** Caller owns the photo viewer, navigation and browser; destroy the destination's ViewModelStore on removal. */
fun createRoasterDetailEntry(
    repository: RoasterRepository,
    favorites: RoasterFavoritesPort = RoasterFavoritesPort.None,
    shopPhotos: RoasterShopPhotos = RoasterShopPhotos.None,
    analytics: RoasterAnalytics = RoasterAnalytics.None,
    screenAnalytics: RoasterScreenAnalytics = RoasterScreenAnalytics.None,
    image: RoasterImage,
    photoViewer: RoasterPhotoViewer,
    onFavoriteFeedback: () -> Unit = {},
): RoasterDetailEntry = RoasterDetailApiImpl(repository, favorites, shopPhotos, analytics, screenAnalytics, image, photoViewer, onFavoriteFeedback)

private class RoasterDiscoveryApiImpl(
    private val repository: RoasterRepository,
    private val favorites: RoasterFavoritesPort,
    private val analytics: RoasterAnalytics,
    private val screenAnalytics: RoasterScreenAnalytics,
    private val image: RoasterImage,
    private val loadError: RoasterLoadError,
    private val emptyContent: @Composable (Modifier) -> Unit,
    private val onFavoriteFeedback: () -> Unit,
) : RoasterDiscoveryEntry {
    private val instanceKey = Uuid.random().toString()

    @Composable
    private fun model(): RoasterListViewModel =
        viewModel(key = "roaster-discovery:$instanceKey") { RoasterListViewModel(repository, favorites, analytics.list()) }

    @Composable
    override fun hasResults(query: String, selectedRoasters: Set<String>, favoritesOnly: Boolean): Boolean? = key(this) {
        val state = roasterDiscoveryState(model(), query, selectedRoasters, favoritesOnly)
        state.hasResults(query, selectedRoasters, favoritesOnly)
    }

    @Composable
    override fun Section(query: String, selectedRoasters: Set<String>, favoritesOnly: Boolean,
        onOpenRoaster: (String) -> Unit, modifier: Modifier) {
        key(this) {
            RoasterDiscoverySection(model(), query, selectedRoasters, favoritesOnly, image, loadError, emptyContent, screenAnalytics,
                onFavoriteFeedback, modifier) { event -> when (event) { is RoasterListEvent.OpenRoaster -> onOpenRoaster(event.slug) } }
        }
    }

    @Composable
    override fun Search(query: String, selectedRoasters: Set<String>, favoritesOnly: Boolean,
        onOpenRoaster: (String) -> Unit, modifier: Modifier, bottomClearance: Dp, header: @Composable ColumnScope.() -> Unit) {
        key(this) {
            RoasterListScreen(model(), query, selectedRoasters, favoritesOnly, image, loadError, emptyContent, screenAnalytics,
                onFavoriteFeedback, bottomClearance, modifier, header) { event ->
                when (event) { is RoasterListEvent.OpenRoaster -> onOpenRoaster(event.slug) }
            }
        }
    }
}

private class RoasterDetailApiImpl(
    private val repository: RoasterRepository,
    private val favorites: RoasterFavoritesPort,
    private val shopPhotos: RoasterShopPhotos,
    private val analytics: RoasterAnalytics,
    private val screenAnalytics: RoasterScreenAnalytics,
    private val image: RoasterImage,
    private val photoViewer: RoasterPhotoViewer,
    private val onFavoriteFeedback: () -> Unit,
) : RoasterDetailEntry {
    private val instanceKey = Uuid.random().toString()

    @Composable
    override fun Content(slug: String, onBack: () -> Unit, onOpenShop: (String) -> Unit, onOpenLink: (String) -> Unit, onShare: (String) -> Unit, modifier: Modifier) {
        key(this, slug) {
            val model = viewModel(key = "roaster-detail:$instanceKey:$slug") {
                RoasterDetailViewModel(slug, repository, favorites, shopPhotos, analytics.detail())
            }
            androidx.compose.runtime.CompositionLocalProvider(com.coffeepeek.feature.roaster.impl.ui.compose.component.LocalRoasterShare provides onShare) {
            RoasterDetailScreen(model, image, photoViewer, screenAnalytics, onFavoriteFeedback, modifier) { event ->
                when (event) {
                    RoasterDetailEvent.Back -> onBack()
                    is RoasterDetailEvent.OpenShop -> onOpenShop(event.slug)
                    is RoasterDetailEvent.OpenLink -> onOpenLink(event.url)
                }
            }
            }
        }
    }
}
