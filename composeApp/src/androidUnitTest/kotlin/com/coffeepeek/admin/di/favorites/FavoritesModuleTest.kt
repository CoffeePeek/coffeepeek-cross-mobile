package com.coffeepeek.admin.di.favorites

import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.CreateRoasterInput
import com.coffeepeek.domain.model.RoasterSubmissionResult
import com.coffeepeek.domain.model.RoasterSubmission
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
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
import org.koin.dsl.module
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesModuleTest {
    @Test fun accountScopedGraphUsesTheExistingRoasterWriterAndLazyCatalogue() = runTest {
        val settings = Settings()
        val sessions = CheckInTestSessions()
        val saved = object : RoasterFavorites {
            val rows = MutableStateFlow(emptyList<CatalogItem>())
            override fun observeFavorites() = rows
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> {
                rows.value = if (isFavorite) listOf(roaster) else emptyList()
                return Result.success(Unit)
            }
            override suspend fun removeForAccount(roaster: CatalogItem, expectedUserId: String): Result<Unit> {
                check(sessions.peekSession()?.userId == expectedUserId)
                return setFavorite(roaster, false)
            }
        }
        saved.setFavorite(CatalogItem("raw-id", "Roaster", photoUrl = "saved.png",
            address = PublicAddress("public", "/roasters/public", 1, false)), true).getOrThrow()
        var catalogueReads = 0
        val catalog = object : RoasterRepository {
            override suspend fun getRoasters(): Result<List<RoasterSummary>> = error("unused")
            override suspend fun getRoaster(id: String): Result<RoasterDetails> { catalogueReads++; return Result.failure(IllegalStateException("unused")) }
            override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("unused")
            override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("unused")
        }
        val app = koinApplication {
            modules(module {
                single<SessionRepository> { sessions }
                single<SessionReader> { sessions }
                single<RoasterFavorites> { saved }
                single<RoasterRepository> { catalog }
            }, favoritesRoomModule(settings, sessionScoped = true))
        }
        try {
            assertNotNull(app.koin.get<FavoritesEntry>())
            assertEquals(0, catalogueReads)
            val source = app.koin.get<FavoriteRoastersRepository>()
            val rows = source.observe().first { it.getOrNull()?.isNotEmpty() == true }.getOrThrow()
            assertEquals("public", rows.single().publicSlug)
            source.remove(rows.single().id).getOrThrow()
            assertTrue(saved.observeFavorites().first().isEmpty())
            assertEquals(0, catalogueReads)
        } finally { app.close() }
    }
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
