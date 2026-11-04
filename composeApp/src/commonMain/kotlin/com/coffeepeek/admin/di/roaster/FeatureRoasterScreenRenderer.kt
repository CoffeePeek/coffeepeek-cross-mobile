package com.coffeepeek.admin.di.roaster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.LoadErrorContent
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.SearchHeader
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.admin.ui.screen.feed.CatalogSuggestionCard
import com.coffeepeek.admin.ui.screen.feed.CatalogSuggestionKind
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.feature.roaster.api.RoasterDetailEntry
import com.coffeepeek.feature.roaster.api.RoasterDiscoveryEntry
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterImage
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.createRoasterDetailEntry
import com.coffeepeek.feature.roaster.impl.createRoasterDiscoveryEntry

/** Both platforms use the migrated roasters; the search header, navigation, browser and viewer stay app-owned. */
internal class FeatureRoasterScreenRenderer(
    private val discovery: RoasterDiscoveryEntry,
    private val detail: RoasterDetailEntry,
) : RoasterScreenRenderer {
    @Composable
    override fun hasDiscoveryResults(query: String, selectedRoasterIds: Set<String>, favoritesOnly: Boolean): Boolean? =
        discovery.hasResults(query, selectedRoasterIds, favoritesOnly)

    @Composable
    override fun DiscoverySection(query: String, selectedRoasterIds: Set<String>, favoritesOnly: Boolean) {
        discovery.Section(query, selectedRoasterIds, favoritesOnly, onOpenRoaster = ::openRoaster, modifier = Modifier)
    }

    @Composable
    override fun DiscoverySearch(onCancel: () -> Unit, query: String, onQueryChange: (String) -> Unit,
        selectedRoasterIds: Set<String>, favoritesOnly: Boolean) {
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        discovery.Search(query, selectedRoasterIds, favoritesOnly, onOpenRoaster = ::openRoaster, modifier = Modifier,
            bottomClearance = LocalFloatingNavClearance.current) {
            SearchHeader(
                query = query, onQueryChange = onQueryChange,
                analyticsCatalog = "roasters", analyticsScreen = "roasters",
                roastersSelected = true, onSelectRoasters = {}, showCategories = false,
                onCancelSearch = {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                    onCancel()
                },
            )
        }
    }

    @Composable
    override fun Detail(roasterId: String) {
        detail.Content(roasterId, onBack = Navigator::popBack,
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenLink = OpenInBrowser::openInBrowser,
            onShare = { com.coffeepeek.admin.utils.ShareHelper.shareText("https://coffeepeek.by$it") }, modifier = Modifier)
    }

    private fun openRoaster(slug: String) = Navigator.navigate(Navigator.Screen.RoasterDetail(slug))
}

internal fun roasterScreenRenderer(
    repository: RoasterRepository,
    favorites: RoasterFavoritesPort,
    shopPhotos: RoasterShopPhotos,
): FeatureRoasterScreenRenderer {
    val image: RoasterImage = { url, description, modifier, scale, labelSize ->
        CoffeeShopImage(imageUrl = url, modifier = modifier, contentDescription = description, contentScale = scale,
            placeholderLabelSize = labelSize)
    }
    val feedback = { platformHaptic(HapticEvent.Selection) }
    return FeatureRoasterScreenRenderer(
        createRoasterDiscoveryEntry(repository, favorites, RoasterAnalyticsAdapter, RoasterScreenAnalyticsAdapter, image,
            loadError = { onRetry -> LoadErrorContent(onRetry) },
            emptyContent = { modifier -> CatalogSuggestionCard(CatalogSuggestionKind.Roaster,
                onAdd = { Navigator.navigate(Navigator.Screen.AddRoaster) }, modifier = modifier) }, onFavoriteFeedback = feedback),
        createRoasterDetailEntry(repository, favorites, shopPhotos, RoasterAnalyticsAdapter, RoasterScreenAnalyticsAdapter, image,
            photoViewer = { url, onDismiss -> FullScreenImageDialog(imageUrl = url, onDismiss = onDismiss) }, onFavoriteFeedback = feedback),
    )
}
