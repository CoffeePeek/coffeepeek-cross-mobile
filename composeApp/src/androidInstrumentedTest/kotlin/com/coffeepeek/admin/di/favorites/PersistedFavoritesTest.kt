package com.coffeepeek.admin.di.favorites

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.domain.repository.FavoriteRepository as LegacyFavoriteRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.room.CoffeePeekDatabase
import com.coffeepeek.room.CoffeePeekDatabase.Companion.configure
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepositoryImp
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class PersistedFavoritesTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun legacyJsonRowsSurviveRoomReopenAndUseOneWriter(): Unit = runBlocking {
        val name = "favorites-integration-${UUID.randomUUID()}.db"
        val key = "local_favorite_shops"
        try {
            val seeded = Room.databaseBuilder<CoffeePeekDatabase>(context, name).configure()
            try {
                SettingRepositoryImp(seeded.settingDAO).save(Setting(key, """[
                    {"id":"saved-a","title":"Старая кофейня","rating":4.8,
                     "latitude":53.9,"longitude":27.56,"roasterPhotoUrl":"logo-a","unknown":true},
                    {"id":"saved-b","title":"Вторая кофейня"}
                ]"""))
            } finally { seeded.close() }

            val reopened = Room.databaseBuilder<CoffeePeekDatabase>(context, name).configure()
            val settings = SettingRepositoryImp(reopened.settingDAO)
            val app = koinApplication {
                modules(favoritesRoomModule(settings, Dispatchers.IO), legacyFavoritesConsumersModule())
            }
            try {
                val current = app.koin.get<FavoritesRepository>()
                val legacy = app.koin.get<LegacyFavoriteRepository>()
                val saved = current.read().getOrThrow()
                assertEquals(listOf("saved-a", "saved-b"), saved.map { it.id })
                assertEquals(53.9, saved.first().latitude)
                assertEquals(listOf("logo-a"), saved.first().roasterPhotoUrls)
                assertEquals(setOf("saved-a", "saved-b"), legacy.getFavoriteIds())

                current.remove("saved-a").getOrThrow()
                assertEquals(setOf("saved-b"), legacy.getFavoriteIds())
                assertNotNull(settings.read(key))
                current.remove("saved-b").getOrThrow()
                assertNull(settings.read(key))
            } finally {
                app.close()
                reopened.close()
            }
        } finally { context.deleteDatabase(name) }
    }
}
