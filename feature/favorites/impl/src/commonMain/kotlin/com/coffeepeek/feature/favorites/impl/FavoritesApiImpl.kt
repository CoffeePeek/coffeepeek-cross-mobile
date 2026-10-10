package com.coffeepeek.feature.favorites.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.impl.ui.compose.FavoritesScreen
import com.coffeepeek.feature.favorites.impl.ui.FavoritesViewModel

/** Manual construction: UI does not fetch anything from Koin or application globals. */
fun createFavoritesEntry(repository: FavoritesRepository): FavoritesEntry = FavoritesApiImpl(repository)

internal class FavoritesApiImpl(private val repository: FavoritesRepository) : FavoritesEntry {
    @Composable
    override fun Content(
        onOpenShop: (String) -> Unit,
        onBack: () -> Unit,
        distanceForCoordinates: (Double, Double) -> String?,
    ) {
        val vm = viewModel { FavoritesViewModel(repository) }
        FavoritesScreen(vm, onOpenShop, onBack, distanceForCoordinates)
    }
}
