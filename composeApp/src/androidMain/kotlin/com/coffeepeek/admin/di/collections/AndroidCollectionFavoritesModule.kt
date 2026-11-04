package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.data.ServerRoasterFavorites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/** Android shows the collection sheet after a roaster heart; iOS keeps the plain writer bound in the root graph. */
internal fun androidCollectionFavoritesModule() = module {
    single { CollectionPickerRequests(get(), CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) }
    single<CollectionPickerPrompts> { get<CollectionPickerRequests>() }
    single<FavoriteCollectionsPrompt> { AndroidFavoriteCollectionsPrompt(get()) }
    single<RoasterFavorites> { CollectionPromptRoasterFavorites(ServerRoasterFavorites(get(), get(), get()), get(), get()) }
}
