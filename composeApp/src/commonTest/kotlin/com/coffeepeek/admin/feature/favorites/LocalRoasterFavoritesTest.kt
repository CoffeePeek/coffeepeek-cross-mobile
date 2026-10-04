package com.coffeepeek.admin.feature.favorites

import com.coffeepeek.admin.feature.favorites.data.LocalRoasterFavorites
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalRoasterFavoritesTest {
    @Test
    fun restoresSnapshotsAndTreatsCatalogAndDetailAsTheSameRoaster() = runBlocking {
        val settings = MemorySettings()
        val sessions = MemorySessions("alice")
        val favorites = LocalRoasterFavorites(settings, sessions)
        val catalog = roaster("public-roaster").copy(id = "catalog-guid", photoUrl = "logo.png")
        favorites.setFavorite(catalog, true).getOrThrow()

        val restored = LocalRoasterFavorites(settings, sessions).observeFavorites().first().single()
        assertEquals("public-roaster", restored.id)
        assertEquals(catalog.address, restored.address)
        assertEquals("logo.png", restored.photoUrl)

        favorites.setFavorite(catalog.copy(id = "public-roaster", name = "Updated name"), true).getOrThrow()
        assertEquals("Updated name", favorites.observeFavorites().first().single().name)
        favorites.setFavorite(restored, false).getOrThrow()
        assertTrue(LocalRoasterFavorites(settings, sessions).observeFavorites().first().isEmpty())
        assertTrue(settings.readAll().isEmpty())
    }

    @Test
    fun observesChangesAndKeepsAccountsAndGuestsSeparate() = runBlocking {
        val sessions = MemorySessions("alice")
        val favorites = LocalRoasterFavorites(MemorySettings(), sessions)
        val updates = Channel<List<CatalogItem>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            favorites.observeFavorites().collect { updates.send(it) }
        }
        suspend fun next() = withTimeout(2_000) { updates.receive() }
        try {
            assertTrue(next().isEmpty())
            favorites.setFavorite(roaster("alice-roaster"), true).getOrThrow()
            assertEquals(listOf("alice-roaster"), next().map { it.id })

            sessions.signIn("bob")
            assertTrue(next().isEmpty())
            favorites.setFavorite(roaster("bob-roaster"), true).getOrThrow()
            assertEquals(listOf("bob-roaster"), next().map { it.id })

            sessions.signIn(null)
            assertTrue(next().isEmpty())
            assertTrue(favorites.setFavorite(roaster("guest-roaster"), true).isFailure)
            sessions.signIn("alice")
            assertEquals(listOf("alice-roaster"), next().map { it.id })
        } finally {
            observer.cancelAndJoin()
            updates.close()
        }
    }

    @Test
    fun simultaneousMutationsDoNotLoseOtherFavorites() = runBlocking {
        val favorites = LocalRoasterFavorites(MemorySettings(), MemorySessions("alice"))
        (0 until 30).map { index ->
            async { favorites.setFavorite(roaster("roaster-$index"), true).getOrThrow() }
        }.awaitAll()
        assertEquals(30, favorites.observeFavorites().first().size)

        (0 until 30 step 2).map { index ->
            async { favorites.setFavorite(roaster("roaster-$index"), false).getOrThrow() }
        }.awaitAll()
        assertEquals((1 until 30 step 2).map { "roaster-$it" }.toSet(), favorites.observeFavorites().first().map { it.id }.toSet())
    }

    @Test
    fun failedWritesKeepTheSavedFavorite() = runBlocking {
        val settings = MemorySettings()
        val favorites = LocalRoasterFavorites(settings, MemorySessions("alice"))
        val roaster = roaster("saved-roaster")
        favorites.setFavorite(roaster, true).getOrThrow()
        settings.failWrites = true
        assertTrue(favorites.setFavorite(roaster, false).isFailure)
        assertEquals(listOf(roaster.id), favorites.observeFavorites().first().map { it.id })
    }

    private fun roaster(slug: String) = CatalogItem(
        id = slug,
        name = slug,
        address = PublicAddress(slug, "/roasters/$slug", 1, false),
    )
}

private class MemorySettings : SettingRepository {
    private val values = MutableStateFlow<Map<String, Setting>>(emptyMap())
    var failWrites = false

    override suspend fun save(model: Setting) {
        check(!failWrites) { "Persistence unavailable" }
        yield()
        values.value = values.value + (model.key to model)
    }

    override suspend fun read(key: String): Setting? = values.value[key]
    override fun readFlow(key: String): Flow<Setting?> = values.map { it[key] }.distinctUntilChanged()
    override suspend fun readAll(): List<Setting> = values.value.values.toList()
    override fun readAllFlow(): Flow<List<Setting>> = values.map { it.values.toList() }

    override suspend fun delete(key: String) {
        check(!failWrites) { "Persistence unavailable" }
        yield()
        values.value = values.value - key
    }
}

private class MemorySessions(userId: String) : SessionRepository {
    private val state = MutableStateFlow<Session?>(Session("test-session", userId = userId))
    fun signIn(userId: String?) { state.value = userId?.let { Session("test-session", userId = it) } }
    override fun peekSession(): Session? = state.value
    override fun applySession(session: Session?) { state.value = session }
    override fun isActiveSession(session: Session?): Boolean = session?.userId != null
    override suspend fun getSession(): Session? = state.value
    override suspend fun persistSession(session: Session?) { state.value = session }
    override suspend fun saveSession(session: Session?) { state.value = session }
    override suspend fun warmCache() = Unit
    override fun observeSession(): Flow<Session?> = state
    override suspend fun isLoggedIn(): Boolean = isActiveSession(state.value)
}
