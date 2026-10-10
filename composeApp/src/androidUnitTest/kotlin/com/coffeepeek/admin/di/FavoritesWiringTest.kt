package com.coffeepeek.admin.di

import com.coffeepeek.data.di.dataModule
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.repository.FavoriteRepository as LegacyFavoriteRepository
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.admin.di.favorites.favoritesRoomModule
import com.coffeepeek.admin.di.favorites.legacyFavoritesConsumersModule
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FavoritesWiringTest {
    private class Settings : SettingRepository {
        private val values = MutableStateFlow(emptyMap<String, String>())
        override suspend fun save(model: Setting) { values.value = values.value + (model.key to model.value) }
        override suspend fun read(key: String) = values.value[key]?.let { Setting(key, it) }
        override fun readFlow(key: String) = values.map { it[key]?.let { value -> Setting(key, value) } }
        override suspend fun readAll() = values.value.map { Setting(it.key, it.value) }
        override fun readAllFlow() = values.map { all -> all.map { Setting(it.key, it.value) } }
        override suspend fun delete(key: String) { values.value = values.value - key }
    }

    @Test fun androidModulesExposeOneLegacyContractBackedByNewRepository() = runBlocking {
        val settings = Settings()
        val database = object : DatabaseCore { override val settingRepository = settings }
        val app = koinApplication {
            modules(
                dataModule("url", "cache", "root", database, registerLegacyFavorites = false),
                favoritesRoomModule(settings, Dispatchers.Unconfined),
                legacyFavoritesConsumersModule(),
            )
        }
        try {
            val legacy = app.koin.getAll<LegacyFavoriteRepository>().single()
            val current = app.koin.get<FavoritesRepository>()
            assertNotNull(app.koin.get<FavoritesEntry>())

            legacy.addFavorite(CoffeeShop("a", "A", null, cityName = null,
                priceRange = null, photoUrl = null)).getOrThrow()
            assertEquals(listOf("a"), current.read().getOrThrow().map { it.id })
            current.remove("a").getOrThrow()
            assertEquals(emptySet(), legacy.getFavoriteIds())

            settings.save(Setting("local_favorite_shops", "corrupt"))
            assertEquals(emptySet(), legacy.getFavoriteIds())
            assertTrue(current.read().isFailure)
            assertTrue(legacy.addFavorite(CoffeeShop("b", "B", null,
                cityName = null, priceRange = null, photoUrl = null)).isFailure)
            assertEquals("corrupt", settings.read("local_favorite_shops")?.value)
        } finally { app.close() }
    }
}
