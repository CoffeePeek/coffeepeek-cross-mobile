package com.coffeepeek.admin.di.favorites

import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import org.koin.dsl.module

/** Only the existing shop contract is shared; this does not activate the Favorites screen or replace legacy consumers. */
internal fun serverFavoriteShopsModule() = module {
    single<FavoritesRepository> {
        val shops = get<ShopRepository>()
        ServerFavoriteShopsRepository(get(), get(), shops::getShopDetails)
    }
}
