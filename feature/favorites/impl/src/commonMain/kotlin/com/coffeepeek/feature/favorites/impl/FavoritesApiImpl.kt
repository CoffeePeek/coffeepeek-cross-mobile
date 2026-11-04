package com.coffeepeek.feature.favorites.impl

import com.coffeepeek.feature.favorites.api.FavoritesScreenAnalytics
import com.coffeepeek.feature.favorites.impl.ui.compose.LocalFavoritesAnalytics
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesAccount
import com.coffeepeek.feature.favorites.impl.ui.compose.FavoritesScreen
import com.coffeepeek.feature.favorites.impl.ui.FavoritesViewModel

/** Manual construction: UI does not fetch anything from Koin or application globals. */
fun createFavoritesEntry(
    repository: FavoritesRepository,
    roasters: FavoriteRoastersRepository? = null,
    drinks: FavoriteDrinksRepository? = null,
    screenAnalytics: FavoritesScreenAnalytics = FavoritesScreenAnalytics.None,
    account: FavoritesAccount? = null,
): FavoritesEntry = FavoritesApiImpl(repository, roasters, drinks, screenAnalytics, account)

internal class FavoritesApiImpl(
    private val repository: FavoritesRepository,
    private val roasters: FavoriteRoastersRepository?,
    private val drinks: FavoriteDrinksRepository? = null,
    private val screenAnalytics: FavoritesScreenAnalytics = FavoritesScreenAnalytics.None,
    private val account: FavoritesAccount? = null,
) : FavoritesEntry {
    @Composable
    override fun Content(
        onOpenShop: (String) -> Unit,
        onBack: () -> Unit,
        distanceForCoordinates: (Double, Double) -> String?,
        onOpenRoaster: (String) -> Unit,
        onShare: () -> Unit,
        onOpenCollections: () -> Unit,
    ) {
        val vm = viewModel { FavoritesViewModel(repository, roasters, drinks, account) }
        CompositionLocalProvider(LocalFavoritesAnalytics provides screenAnalytics) {
            FavoritesScreen(vm, onOpenShop, onBack, distanceForCoordinates, onOpenRoaster, onShare, onOpenCollections)
        }
    }
}
