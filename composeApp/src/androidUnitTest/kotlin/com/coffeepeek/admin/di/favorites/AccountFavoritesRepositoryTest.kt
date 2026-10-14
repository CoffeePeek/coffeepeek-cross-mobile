package com.coffeepeek.admin.di.favorites

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.favorites.data.local.FavoritesStorage
import com.coffeepeek.feature.favorites.data.repository.createFavoritesRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountFavoritesRepositoryTest {
    private class Sessions : SessionRepository {
        val state = MutableStateFlow<Session?>(Session("access", "refresh", "A"))
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session != null
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) = Unit
        override suspend fun saveSession(session: Session?) { state.value = session }
        override suspend fun warmCache() = Unit
        override fun observeSession() = state
        override suspend fun isLoggedIn() = state.value != null
    }
    private class Storage : FavoritesStorage {
        val rows = MutableStateFlow(emptyMap<String, String>())
        override suspend fun read(key: String) = rows.value[key]
        override fun observe(key: String) = rows.map { it[key] }
        override suspend fun write(key: String, value: String?) {
            rows.value = if (value == null) rows.value - key else rows.value + (key to value)
        }
    }

    @Test fun switchingToCorruptAccountStorageClearsPreviousCardsBeforeFailure() = runTest {
        val store = Storage()
        val sessions = Sessions()
        val repo = AccountFavoritesRepository(store, sessions, UnconfinedTestDispatcher(testScheduler))
        repo.save(FavoriteShop("private-A", "Private A")).getOrThrow()
        val emissions = mutableListOf<Result<List<FavoriteShop>>>()
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.observe().collect { emissions += it } }
        assertEquals("private-A", emissions.last().getOrThrow().single().id)
        store.write("local_favorite_shops:B", "broken JSON")
        val previousCount = emissions.size
        sessions.saveSession(Session("B", "B-refresh", "B"))
        assertTrue(emissions.drop(previousCount).first().getOrThrow().isEmpty())
        assertTrue(emissions.last().isFailure)
        assertTrue(emissions.drop(previousCount).none { it.getOrNull()?.any { shop -> shop.id == "private-A" } == true })
        observer.cancel()
    }

    @Test fun logoutAndAnotherAccountCannotReadOrMutatePreviousFavorites() = runTest {
        val store = Storage()
        val sessions = Sessions()
        val repo = AccountFavoritesRepository(store, sessions, UnconfinedTestDispatcher(testScheduler))
        repo.save(FavoriteShop("coffee", "Coffee")).getOrThrow()
        sessions.saveSession(null)
        assertTrue(repo.read().getOrThrow().isEmpty())
        assertTrue(repo.observe().first().getOrThrow().isEmpty())
        assertTrue(repo.save(FavoriteShop("guest", "Guest")).isFailure)
        sessions.saveSession(Session("B-token", "B-refresh", "B"))
        assertTrue(repo.read().getOrThrow().isEmpty())
        repo.save(FavoriteShop("B-shop", "B shop")).getOrThrow()
        assertEquals(listOf("B-shop"), repo.read().getOrThrow().map { it.id })
        sessions.saveSession(Session("A-token", "A-refresh", "A"))
        assertEquals(listOf("coffee"), repo.read().getOrThrow().map { it.id })
    }

    @Test fun legacyMigrationRetainsSignedInFavoritesAndCleanupUsesCapturedOwner() = runTest {
        val store = Storage()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        createFavoritesRepository(store, dispatcher).save(FavoriteShop("old", "Old favorite")).getOrThrow()
        val sessions = Sessions()
        val repo = AccountFavoritesRepository(store, sessions, dispatcher)
        assertEquals(listOf("old"), repo.read().getOrThrow().map { it.id })
        assertNull(store.rows.value["local_favorite_shops"])
        sessions.saveSession(null)
        createLegacyFavoritesRepositoryBridge(repo).clearForUser("A")
        sessions.saveSession(Session("access", "refresh", "A"))
        assertTrue(repo.read().getOrThrow().isEmpty())
    }
}
