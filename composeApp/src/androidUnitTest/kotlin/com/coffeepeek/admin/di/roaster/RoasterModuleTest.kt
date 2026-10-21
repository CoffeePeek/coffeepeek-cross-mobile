package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.time.LocalDate
import kotlin.test.*

class RoasterModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun moduleBuildsRepositoryOverTheAuthenticatedClientWithTheLegacyCacheSlotPortsAndRenderer() = runTest {
        val backend = SandboxBackend(byteArrayOf(1), today = LocalDate.parse("2026-10-18"))
        val client = CoffeePeekClient(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
            getToken = { null }, saveToken = { error("No credential writes") },
            apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)
        val settings = MemorySettings()
        val favorites = object : RoasterFavorites {
            override fun observeFavorites() = MutableStateFlow(emptyList<com.coffeepeek.domain.model.CatalogItem>())
            override suspend fun setFavorite(roaster: com.coffeepeek.domain.model.CatalogItem, isFavorite: Boolean) = Result.success(Unit)
        }
        val shops = object : ShopRepository by throwingShops() {}
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val app = koinApplication {
            modules(module {
                single { client }; single<RoasterFavorites> { favorites }; single<ShopRepository> { shops }; single { scope }
            }, roasterModule(SandboxBackend.BASE_URL) { settings })
        }
        try {
            val repository = app.koin.get<RoasterRepository>()
            assertSame(repository, app.koin.get<RoasterRepository>())
            val list = repository.getRoasters().getOrThrow()
            assertEquals(listOf("sandbox-roaster", "sandbox-light", "sandbox-decaf"), list.map { it.slug })
            // The catalog was persisted under the legacy slot.
            assertEquals(setOf(roasterCatalogCacheKey(SandboxBackend.BASE_URL)), settings.values.keys)
            assertEquals("Sandbox Roaster", repository.getRoaster("sandbox-roaster").getOrThrow().name)
            assertIs<RoasterFavoritesPort>(app.koin.get<RoasterFavoritesPort>())
            assertIs<RoasterShopPhotos>(app.koin.get<RoasterShopPhotos>())
            assertNotNull(roasterScreenRenderer(repository, app.koin.get(), app.koin.get()))
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            app.close(); client.client.close(); client.plainClient.close(); client.uploadClient.close(); client.imageClient.close(); backend.close()
        }
    }

    private fun throwingShops(): ShopRepository = FakeShopsForModule
}

private object FakeShopsForModule : ShopRepository {
    override suspend fun searchShops(filters: com.coffeepeek.domain.model.ShopFilters) = TODO()
    override suspend fun getShopDetails(id: String) = TODO()
    override suspend fun getMapContent(bounds: com.coffeepeek.domain.model.MapBounds, zoom: Float, filters: com.coffeepeek.domain.model.ShopFilters) = TODO()
    override suspend fun getCatalogs() = TODO()
    override suspend fun getConsumedDrinks() = TODO()
    override suspend fun getMenuDrinks() = TODO()
    override suspend fun createShop(input: com.coffeepeek.domain.model.CreateShopInput) = TODO()
    override suspend fun getMyShopSubmissions(status: com.coffeepeek.domain.model.ModerationStatus, page: Int, pageSize: Int) = TODO()
}
