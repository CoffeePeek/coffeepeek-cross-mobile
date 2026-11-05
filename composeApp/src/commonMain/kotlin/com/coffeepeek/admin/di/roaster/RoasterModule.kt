package com.coffeepeek.admin.di.roaster

import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.admin.di.favorites.RoasterFavorites
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import com.coffeepeek.feature.roaster.data.repository.createPublicRoasterCatalog
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.data.repository.RoasterCatalogStore
import com.coffeepeek.feature.roaster.data.repository.createRoasterRepository
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The catalogue cache keeps its persisted slot, so saved data survives updates. */
internal class SettingRoasterCatalogStore(private val settings: SettingRepository, private val key: String) : RoasterCatalogStore {
    override suspend fun read(): String? = settings.read(key)?.value?.takeIf { it.isNotBlank() && it != "null" }
    override suspend fun write(value: String) = settings.save(Setting(key, value))
}

internal fun roasterCatalogCacheKey(baseUrl: String) = "roaster_catalog_v1:${baseUrl.trimEnd('/')}"

/** App-owned assembly of the migrated roasters; the authenticated client keeps its lifetime and policy. */
@OptIn(ExperimentalTime::class)
internal fun roasterModule(baseUrl: String = Constants.BASE_URL, settings: () -> SettingRepository = { Locator.database.settingRepository }) = module {
    single<RoasterCatalogStore> { SettingRoasterCatalogStore(settings(), roasterCatalogCacheKey(baseUrl)) }
    single<RoasterRepository> {
        createRoasterRepository(get<ApiClients>().client, baseUrl, get()) { Clock.System.now().toEpochMilliseconds() }
    }
    single<PublicRoasterCatalog> { createPublicRoasterCatalog(get()) }
    single<RoasterFavoritesPort> { RoasterFavoritesAdapter(get<RoasterFavorites>(), get<CoroutineScope>(),
        getOrNull<FavoriteCollectionsPrompt>(), getOrNull<AccountFavorites>()) }
    single<RoasterShopPhotos> { RoasterShopPhotosAdapter(get<PublicShopPreviews>()) }
}
