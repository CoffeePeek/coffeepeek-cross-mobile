package com.coffeepeek.feature.favorites.domain.usecase

import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ObserveFavoriteIdsTest {
    @Test fun membershipDeduplicatesIdsAndMetadataOnlyUpdatesButPreservesFailure() = runTest {
        val failure = IllegalStateException("unavailable")
        val repo = object : FavoritesRepository {
            override fun observe() = flowOf(
                Result.success(listOf(FavoriteShop("a", "A"), FavoriteShop("a", "Duplicate"))),
                Result.success(listOf(FavoriteShop("a", "Updated"))),
                Result.failure<List<FavoriteShop>>(failure), Result.success(emptyList()))
            override suspend fun read(): Result<List<FavoriteShop>> = error("not used")
            override suspend fun save(shop: FavoriteShop): Result<Unit> = error("not used")
            override suspend fun remove(shopId: String): Result<Unit> = error("not used")
            override suspend fun clear(): Result<Unit> = error("not used")
        }
        val results = ObserveFavoriteIdsUseCase(repo)().toList()
        assertEquals(3, results.size)
        assertEquals(setOf("a"), results.first().getOrThrow())
        assertSame(failure, results[1].exceptionOrNull())
        assertEquals(emptySet(), results.last().getOrThrow())
    }
}
