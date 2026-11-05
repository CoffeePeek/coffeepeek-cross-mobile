package com.coffeepeek.admin.session

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserSessionCleanerTest {
    @Test fun sessionObserverCancellationCannotInterruptCleanup() = runTest {
        val operations = mutableListOf<String>()
        lateinit var cleanup: Job
        val cleaner = UserSessionCleaner(
            FakeSessionStore { cleanup.cancel(); yield(); operations += "session" },
            clearFavoritesCache = { yield(); operations += "favorites" },
            clearCaches = { operations += "caches" },
        )
        cleanup = launch(start = CoroutineStart.LAZY) { cleaner.clear() }
        cleanup.start(); cleanup.join()
        assertEquals(listOf("session", "favorites", "caches"), operations)
    }

    @Test fun cacheCleanupRunsAndFavoriteFailureIsPreserved() = runTest {
        val operations = mutableListOf<String>()
        val cleaner = UserSessionCleaner(
            sessions = FakeSessionStore { operations += "session" },
            clearFavoritesCache = {
                operations += "favorites"
                error("favorite storage unavailable")
            },
            clearCaches = { operations += "caches" },
        )

        val failure = assertFailsWith<IllegalStateException> { cleaner.clear() }

        assertEquals("favorite storage unavailable", failure.message)
        assertEquals(listOf("session", "favorites", "caches"), operations)
    }

    @Test fun aSessionFailureStillClearsTheCachesAndSkipsTheFavorites() = runTest {
        val operations = mutableListOf<String>()
        val cleaner = UserSessionCleaner(
            sessions = FakeSessionStore { operations += "session"; error("secure store unavailable") },
            clearFavoritesCache = { operations += "favorites" },
            clearCaches = { operations += "caches" },
        )

        val failure = assertFailsWith<IllegalStateException> { cleaner.clear() }

        assertEquals("secure store unavailable", failure.message)
        assertEquals(listOf("session", "caches"), operations)
    }
}

private class FakeSessionStore(
    private val onSave: suspend () -> Unit,
) : SessionStore {
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
