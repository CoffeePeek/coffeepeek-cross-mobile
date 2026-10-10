package com.coffeepeek.admin.di.favorites

import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.usecase.ObserveFavoriteIdsUseCase
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.repository.FavoriteRepository as LegacyFavoriteRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesModuleTest {
    private class Settings : SettingRepository {
        val values = MutableStateFlow(mapOf("other_key" to "preserved"))
        override suspend fun save(model: Setting) { values.value = values.value + (model.key to model.value) }
        override suspend fun read(key: String) = values.value[key]?.let { Setting(key, it) }
        override fun readFlow(key: String) = values.map { it[key]?.let { value -> Setting(key, value) } }
        override suspend fun readAll() = values.value.map { Setting(it.key, it.value) }
        override fun readAllFlow() = values.map { all -> all.map { Setting(it.key, it.value) } }
        override suspend fun delete(key: String) { values.value = values.value - key }
    }

    @Test fun isolatedKoinGraphResolvesSharedRepositoryScreenAndMembershipUseCase() = runTest {
        val settings = Settings()
        val app = koinApplication { modules(favoritesRoomModule(settings, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler))) }
        try {
            val repo = app.koin.get<FavoritesRepository>()
            assertSame(repo, app.koin.get<FavoritesRepository>())
            assertNotNull(app.koin.get<FavoritesEntry>())
            repo.save(FavoriteShop("a", "A")).getOrThrow()
            assertEquals(setOf("a"), app.koin.get<ObserveFavoriteIdsUseCase>()().first().getOrThrow())
            assertNotNull(settings.values.value["local_favorite_shops"])
            repo.clear().getOrThrow()
            assertEquals(mapOf("other_key" to "preserved"), settings.values.value)
        } finally { app.close() }
    }

    @Test fun legacyConsumerBindingUsesTheSameNewRepository() = runTest {
        val settings = Settings()
        val app = koinApplication {
            modules(
                favoritesRoomModule(settings, kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                legacyFavoritesConsumersModule(),
            )
        }
        try {
            val current = app.koin.get<FavoritesRepository>()
            val legacy = app.koin.get<LegacyFavoriteRepository>()
            legacy.addFavorite(CoffeeShop("a", "A", null, cityName = null,
                priceRange = null, photoUrl = null)).getOrThrow()
            assertEquals(listOf("a"), current.read().getOrThrow().map { it.id })
            current.remove("a").getOrThrow()
            assertEquals(emptySet(), legacy.getFavoriteIds())
        } finally { app.close() }
    }
}
