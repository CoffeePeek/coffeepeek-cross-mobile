package com.coffeepeek.feature.favorites.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import com.coffeepeek.feature.favorites.impl.ui.compose.FavoritesScreen
import com.coffeepeek.feature.favorites.impl.ui.FavoritesViewModel

/** Manual construction: UI does not fetch anything from Koin or application globals. */
fun createFavoritesEntry(
    repository: FavoritesRepository,
    roasters: FavoriteRoastersRepository? = null,
    drinks: FavoriteDrinksRepository? = null,
): FavoritesEntry = FavoritesApiImpl(repository, roasters, drinks)

internal class FavoritesApiImpl(
    private val repository: FavoritesRepository,
    private val roasters: FavoriteRoastersRepository?,
    private val drinks: FavoriteDrinksRepository? = null,
) : FavoritesEntry {
    @Composable
    override fun Content(
        onOpenShop: (String) -> Unit,
        onBack: () -> Unit,
        distanceForCoordinates: (Double, Double) -> String?,
        onOpenRoaster: (String) -> Unit,
    ) {
        val vm = viewModel { FavoritesViewModel(repository, roasters, drinks) }
        FavoritesScreen(vm, onOpenShop, onBack, distanceForCoordinates, onOpenRoaster)
    }
}
