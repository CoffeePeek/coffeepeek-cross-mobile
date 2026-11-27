package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.di.favorites.RoasterFavorites
import com.coffeepeek.admin.di.favorites.ServerRoasterFavorites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/** Both platforms route shop and roaster hearts through the same membership picker. */
internal fun collectionFavoritesModule() = module {
    single { CollectionPickerRequests(get(), CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), get()) }
    single<CollectionPickerPrompts> { get<CollectionPickerRequests>() }
    single<FavoriteCollectionsPrompt> { CollectionFavoritesPrompt(get()) }
    single<RoasterFavorites> { CollectionPromptRoasterFavorites(ServerRoasterFavorites(get(), get(), get()), get(), get()) }
}
