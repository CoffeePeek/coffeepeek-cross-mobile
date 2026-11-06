package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.di.shop.shopBrowsingModule
import com.coffeepeek.admin.di.favorites.RoasterFavorites
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.time.LocalDate
import kotlin.test.*

class RoasterModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun migratedRoasterHeartOpensPickerForBothStatesWithoutFavoriteWrite() {
        val selections = mutableListOf<String>()
        var writes = 0
        val favorites = object : RoasterFavorites {
            override fun observeFavorites() = MutableStateFlow(emptyList<com.coffeepeek.admin.di.favorites.CatalogItem>())
            override suspend fun setFavorite(roaster: com.coffeepeek.admin.di.favorites.CatalogItem, isFavorite: Boolean): Result<Unit> {
                writes++
                return Result.success(Unit)
            }
        }
        val prompt = object : FavoriteCollectionsPrompt {
            override fun shop(slug: String) = Unit
            override fun roaster(slug: String) { selections += slug }
        }
        val adapter = RoasterFavoritesAdapter(favorites, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined), prompt)
        val target = RoasterFavoriteTarget("roaster", "Roaster", null, "/roasters/roaster", 1, false)
        adapter.change(target, true) { error("No mutation failure") }
        adapter.change(target, false) { error("No mutation failure") }
        assertEquals(listOf("roaster", "roaster"), selections)
        assertEquals(0, writes)
    }

    @Test fun moduleBuildsPublicReadersAndRendererWithoutExistingShopRepository() = runTest {
        val fixture = fixture()
        try {
            val koin = fixture.app.koin
            val repository = koin.get<RoasterRepository>()
            assertSame(repository, koin.get<RoasterRepository>())
            val slugs = repository.getRoasters().getOrThrow().map { it.slug }
            assertEquals(listOf("sandbox-roaster", "sandbox-light", "sandbox-decaf"), slugs)
            assertEquals(setOf(roasterCatalogCacheKey(SandboxBackend.BASE_URL)), fixture.settings.values.keys)
            assertEquals("Sandbox Roaster", repository.getRoaster("sandbox-roaster").getOrThrow().name)
            assertEquals(slugs, koin.get<PublicRoasterCatalog>().load().getOrThrow().map { it.slug })
            val photo = koin.get<PublicShopPreviews>().load("sandbox-cafe").getOrThrow().detailPhotoUrl
            assertNotNull(photo)
            assertEquals(photo, koin.get<RoasterShopPhotos>().firstPhoto("sandbox-cafe"))
            assertIs<RoasterFavoritesPort>(koin.get<RoasterFavoritesPort>())
            assertNotNull(roasterScreenRenderer(repository, koin.get(), koin.get(), koin.get()))
        } finally { fixture.close() }
    }

    private fun fixture(): Fixture {
        val backend = SandboxBackend(byteArrayOf(1), today = LocalDate.parse("2026-10-18"))
        val client = ApiClients(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
            credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { return null }
                override suspend fun save(tokens: SessionTokens?) { error("No credential writes") }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } },
            apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)
        val settings = MemorySettings()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val app = koinApplication {
            modules(module {
                single { client }
                single<RoasterFavorites> { FakeFavorites() }
                single<SessionReader> { GuestSession }
                single { scope }
            }, roasterModule(SandboxBackend.BASE_URL) { settings }, shopBrowsingModule(SandboxBackend.BASE_URL))
        }
        return Fixture(backend, client, settings, scope, app)
    }

    private object GuestSession : SessionReader {
        override fun peekSession() = null
        override fun isActiveSession(session: Session?) = false
        override fun observeSession() = flowOf(null)
        override suspend fun getSession() = null
    }

    private class FakeFavorites : RoasterFavorites {
        override fun observeFavorites() = MutableStateFlow(emptyList<com.coffeepeek.admin.di.favorites.CatalogItem>())
        override suspend fun setFavorite(roaster: com.coffeepeek.admin.di.favorites.CatalogItem, isFavorite: Boolean) = Result.success(Unit)
    }

    private class Fixture(
        val backend: SandboxBackend, val client: ApiClients, val settings: MemorySettings,
        val scope: CoroutineScope, val app: KoinApplication,
    ) {
        fun close() {
            scope.cancel()
            app.close()
            listOf(client.client, client.plainClient, client.uploadClient, client.imageClient).forEach { it.close() }
            backend.close()
        }
    }
}
