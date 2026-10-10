package com.coffeepeek.data.session

import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserSessionCleanerTest {
    @Test
    fun cacheCleanupRunsAndFavoriteFailureIsPreserved() = runTest {
        val operations = mutableListOf<String>()
        val cleaner = UserSessionCleaner(
            sessionRepository = FakeSessionRepository { operations += "session" },
            favoriteRepository = FailingFavoriteRepository {
                operations += "favorites"
                error("favorite storage unavailable")
            },
            httpCacheFolderPath = "unused",
            appCacheRootPath = "unused",
            clearCaches = { operations += "caches" },
        )

        val failure = assertFailsWith<IllegalStateException> { cleaner.clearLocalUserData() }

        assertEquals("favorite storage unavailable", failure.message)
        assertEquals(listOf("session", "favorites", "caches"), operations)
    }
}

private class FakeSessionRepository(
    private val onSave: suspend () -> Unit,
) : SessionRepository {
    override fun peekSession(): Session? = null
    override fun applySession(session: Session?) = Unit
    override fun isActiveSession(session: Session?) = false
    override suspend fun getSession(): Session? = null
    override suspend fun persistSession(session: Session?) = Unit
    override suspend fun saveSession(session: Session?) = onSave()
    override suspend fun warmCache() = Unit
    override fun observeSession(): Flow<Session?> = emptyFlow()
    override suspend fun isLoggedIn() = false
}

private class FailingFavoriteRepository(
    private val onClear: suspend () -> Unit,
) : FavoriteRepository {
    override suspend fun getFavoriteIds(): Set<String> = emptySet()
    override suspend fun isFavorite(shopId: String) = false
    override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = Result.success(emptyList())
    override suspend fun addFavorite(shop: CoffeeShop, address: String?) = Result.success(Unit)
    override suspend fun removeFavorite(shopId: String) = Result.success(Unit)
    override suspend fun clearAll() = onClear()
}
