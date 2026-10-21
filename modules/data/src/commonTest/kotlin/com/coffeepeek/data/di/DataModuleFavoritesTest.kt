package com.coffeepeek.data.di

import com.coffeepeek.data.repository.FavoriteRepositoryImpl
import com.coffeepeek.data.session.RoomSessionSecureStore
import com.coffeepeek.data.session.SessionSecureStore
import org.koin.dsl.module
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.emptyFlow
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

class DataModuleFavoritesTest {
    private val database = object : DatabaseCore {
        override val settingRepository = object : SettingRepository {
            override suspend fun save(model: Setting) = error("unused")
            override suspend fun read(key: String): Setting? = error("unused")
            override fun readFlow(key: String) = emptyFlow<Setting?>()
            override suspend fun readAll(): List<Setting> = error("unused")
            override fun readAllFlow() = emptyFlow<List<Setting>>()
            override suspend fun delete(key: String) = error("unused")
        }
    }

    @Test fun oldBindingRemainsDefaultForIosAndExistingCallers() {
        val app = koinApplication {
            modules(dataModule("url", "cache", "root", database), module {
                // The legacy favorite writer now resolves session identity as production does.
                // This isolated JVM graph supplies a test store rather than an Android Context.
                single<SessionSecureStore> { RoomSessionSecureStore(database.settingRepository) }
            })
        }
        try {
            assertIs<FavoriteRepositoryImpl>(app.koin.get<FavoriteRepository>())
        } finally { app.close() }
    }

    @Test fun androidCanOmitOldBindingBeforeLoadingFeatureBridge() {
        val app = koinApplication {
            modules(dataModule("url", "cache", "root", database, registerLegacyFavorites = false))
        }
        try {
            assertNull(app.koin.getOrNull<FavoriteRepository>())
        } finally { app.close() }
    }
}
