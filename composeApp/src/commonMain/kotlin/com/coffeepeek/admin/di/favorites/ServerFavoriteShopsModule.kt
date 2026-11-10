package com.coffeepeek.admin.di.favorites

import com.coffeepeek.feature.shop.api.PublicShopDetails
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.admin.di.collections.CollectionPickerPrompts
import org.koin.dsl.module

/** Only the shop contract is shared here; it does not activate the Favorites screen. */
internal fun serverFavoriteShopsModule() = module {
    single<FavoritesRepository> {
        val shops = get<PublicShopDetails>()
        ServerFavoriteShopsRepository(get(), get(), getOrNull<CollectionPickerPrompts>(), shops::load)
    }
}
