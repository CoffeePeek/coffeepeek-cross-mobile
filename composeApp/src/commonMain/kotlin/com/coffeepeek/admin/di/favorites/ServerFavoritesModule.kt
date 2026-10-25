package com.coffeepeek.admin.di.favorites

import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.domain.usecase.ObserveFavoriteIdsUseCase
import com.coffeepeek.feature.favorites.impl.createFavoritesEntry
import org.koin.dsl.module

/** Both platforms adapt the existing server source; local storage is never registered here. */
internal fun serverFavoritesModule() = module {
    includes(serverFavoriteShopsModule())
    factory { ObserveFavoriteIdsUseCase(get()) }
    single<FavoriteRoastersRepository> {
        val catalog = get<RoasterRepository>()
        LegacyFavoriteRoastersRepository(get(), get()) { slug ->
            catalog.getRoaster(slug).map { it.photos.firstOrNull()?.fullUrl }
        }
    }
    single<FavoriteDrinksRepository> { ServerFavoriteDrinksRepository(get(), get()) }
    single<FavoritesEntry> { createFavoritesEntry(get(), get(), get(), AppFavoritesScreenAnalytics) }
}
