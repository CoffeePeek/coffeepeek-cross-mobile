package com.coffeepeek.data.di

import com.coffeepeek.data.repository.FavoriteRepositoryImpl
import com.coffeepeek.core.session.InMemorySessionSecureStore
import com.coffeepeek.core.session.SessionSecureStore
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import org.koin.dsl.module
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
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
            modules(dataModule("url", database), module {
                // The legacy favorite writer now resolves session identity as production does.
                // This isolated JVM graph supplies a test store rather than an Android Context.
                single<SessionSecureStore> { InMemorySessionSecureStore() }
                // The account favorites are provided by the application, not by this module.
                single<FavoritesRepository> { UnusedAccountFavorites }
                single { unusedTransport() }
            })
        }
        try {
            assertIs<FavoriteRepositoryImpl>(app.koin.get<FavoriteRepository>())
        } finally { app.close() }
    }

    @Test fun androidCanOmitOldBindingBeforeLoadingFeatureBridge() {
        val app = koinApplication {
            modules(dataModule("url", database, registerLegacyFavorites = false))
        }
        try {
            assertNull(app.koin.getOrNull<FavoriteRepository>())
        } finally { app.close() }
    }
}

private fun unusedTransport() = ApiClients(
    "https://unused.example", "cache", false,
    credentials = object : SessionCredentials {
        override suspend fun load(): SessionTokens? = null
        override suspend fun save(tokens: SessionTokens?) = Unit
        override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?) = false
    },
    refreshWith = { { Result.failure(IllegalStateException("unused")) } },
    apiClientFactory = { block -> HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }, block) },
    uploadClientFactory = { block -> HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }, block) },
)

private object UnusedAccountFavorites : FavoritesRepository {
    override fun observeFavorites(): Flow<Result<List<FavoriteEntry>>> = error("unused")
    override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> = error("unused")
    override suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean): Result<Unit> = error("unused")
    override fun clearCache() = Unit
}
