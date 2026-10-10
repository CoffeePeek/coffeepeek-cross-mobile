package com.coffeepeek.feature.favorites.domain.usecase

import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Membership projection for catalog/detail/session consumers, without UI snapshots or event buses. */
class ObserveFavoriteIdsUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(): Flow<Result<Set<String>>> = repository.observe()
        .map { result -> result.map { shops -> shops.mapTo(linkedSetOf()) { it.id }.toSet() } }
        .distinctUntilChanged()
}
