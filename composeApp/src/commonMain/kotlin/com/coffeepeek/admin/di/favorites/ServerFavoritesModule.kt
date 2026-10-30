package com.coffeepeek.admin.di.favorites


import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository as LegacyFavorites
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.data.repository.createAccountFavorites
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.domain.usecase.ObserveFavoriteIdsUseCase
import com.coffeepeek.feature.favorites.impl.createFavoritesEntry
import com.coffeepeek.feature.roaster.api.PublicRoasterDetails
import org.koin.dsl.module

/** Both platforms adapt the existing server source; local storage is never registered here. */
internal fun serverFavoritesModule() = module {
    single<AccountFavorites> { createAccountFavorites(get<ApiClients>().client, get()) }
    single<LegacyFavorites> { LegacyAccountFavorites(get()) }
    includes(serverFavoriteShopsModule())
    factory { ObserveFavoriteIdsUseCase(get()) }
    single<FavoriteRoastersRepository> {
        val roasters = get<PublicRoasterDetails>()
        LegacyFavoriteRoastersRepository(get(), get()) { slug -> roasters.load(slug).map { it.photoUrl } }
    }
    single<FavoriteDrinksRepository> { ServerFavoriteDrinksRepository(get(), get()) }
    single<FavoritesEntry> { createFavoritesEntry(get(), get(), get(), AppFavoritesScreenAnalytics) }
}
