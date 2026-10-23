package com.coffeepeek.data.repository

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.data.session.SessionSecureStore
import com.coffeepeek.domain.model.Session
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.*

/** Behaviour contract of the session store, kept identical when it moves to core/session. */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalEncodingApi::class)
class SessionRepositoryParityTest {
    private class Store(initial: AuthResp? = null) : SessionSecureStore {
        val state = MutableStateFlow(initial)
        var writes = 0
        override suspend fun read(): AuthResp? = state.value
        override suspend fun write(data: AuthResp?) { writes++; state.value = data }
        override fun observe() = state
    }
    private class Settings(vararg initial: Pair<String, String>) : SettingRepository {
        val rows = initial.toMap().toMutableMap()
        override suspend fun save(model: Setting) { rows[model.key] = model.value }
        override suspend fun read(key: String): Setting? = rows[key]?.let { Setting(key, it) }
        override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
        override suspend fun readAll() = rows.map { Setting(it.key, it.value) }
        override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
        override suspend fun delete(key: String) { rows.remove(key) }
    }
    private fun database(settings: Settings) = object : DatabaseCore { override val settingRepository = settings }
    private fun jwt(sub: String) = "h." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        .encode("""{"sub":"$sub"}""".encodeToByteArray()) + ".s"
    private fun json(token: String, refresh: String) = """{"accessToken":"$token","refreshToken":"$refresh"}"""

    @Test fun storedCredentialsBecomeASessionWithTheUserIdFromTheToken() = runTest {
        val token = jwt("owner")
        val repository = SessionRepositoryImpl(database(Settings()), Store(AuthResp(token, "refresh")))
        assertNull(repository.peekSession())
        assertEquals(Session(token, "refresh", "owner"), repository.getSession())
        assertEquals(Session(token, "refresh", "owner"), repository.peekSession())
        assertTrue(repository.isLoggedIn())
    }

    @Test fun nothingStoredMeansSignedOut() = runTest {
        val repository = SessionRepositoryImpl(database(Settings()), Store())
        assertNull(repository.getSession())
        assertFalse(repository.isLoggedIn())
    }

    @Test fun oldSettingsRowsAreImportedOnceAndDeleted() = runTest {
        val token = jwt("legacy")
        val settings = Settings("SESSION" to json(token, "r1"), "API_KEYS" to json("older", "r0"))
        val store = Store()
        val repository = SessionRepositoryImpl(database(settings), store)
        assertEquals(Session(token, "r1", "legacy"), repository.getSession())
        assertEquals(AuthResp(token, "r1"), store.state.value)
        assertTrue(settings.rows.isEmpty())
        assertEquals(1, store.writes)
        repository.getSession()
        assertEquals(1, store.writes)
    }

    @Test fun apiKeysRowIsTheFallbackWhenThereIsNoSessionRow() = runTest {
        val token = jwt("keys")
        val settings = Settings("API_KEYS" to json(token, "r"))
        val repository = SessionRepositoryImpl(database(settings), Store())
        assertEquals("keys", repository.getSession()?.userId)
        assertTrue(settings.rows.isEmpty())
    }

    @Test fun secureStoreWinsAndLeavesOldRowsAlone() = runTest {
        val settings = Settings("SESSION" to json(jwt("old"), "r"))
        val repository = SessionRepositoryImpl(database(settings), Store(AuthResp(jwt("current"), "r")))
        assertEquals("current", repository.getSession()?.userId)
        assertTrue("SESSION" in settings.rows)
    }

    @Test fun unreadableOldRowIsIgnored() = runTest {
        val repository = SessionRepositoryImpl(database(Settings("SESSION" to "not json")), Store())
        assertNull(repository.getSession())
    }

    @Test fun saveWritesTheSecureStoreWhileApplyOnlyChangesMemory() = runTest {
        val store = Store()
        val repository = SessionRepositoryImpl(database(Settings()), store)
        val first = Session("a", null, "u")
        repository.applySession(first)
        assertEquals(first, repository.peekSession())
        assertNull(store.state.value)
        repository.persistSession(first)
        assertEquals(AuthResp("a", ""), store.state.value)
        val second = Session("b", "r", "u")
        repository.saveSession(second)
        assertEquals(AuthResp("b", "r"), store.state.value)
        repository.saveSession(null)
        assertNull(store.state.value)
        assertNull(repository.peekSession())
    }

    @Test fun replaceSwapsOnlyWhileTheExpectedSessionIsCurrent() = runTest {
        val store = Store()
        val repository = SessionRepositoryImpl(database(Settings()), store)
        val current = Session("a", "r", "u")
        repository.saveSession(current)
        assertFalse(repository.replaceSession(Session("other", "r", "u"), Session("x", "r", "u")))
        assertEquals(current, repository.peekSession())
        val renewed = current.copy(accessToken = "b")
        assertTrue(repository.replaceSession(current, renewed))
        assertEquals(renewed, repository.peekSession())
        assertEquals("b", store.state.value?.accessToken)
        assertTrue(repository.replaceSession(renewed, null))
        assertNull(store.state.value)
    }

    @Test fun observeStartsWithTheLoadedSessionAndEmitsOnlyChanges() = runTest {
        val repository = SessionRepositoryImpl(database(Settings()), Store())
        val seen = mutableListOf<Session?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeSession().toList(seen) }
        runCurrent()
        val one = Session("a", "r", "u")
        repository.saveSession(one)
        repository.saveSession(one.copy())
        repository.saveSession(null)
        runCurrent()
        job.cancel()
        assertEquals(listOf(null, one, null), seen)
    }

    @Test fun activityRulesComeFromSessionAuth() = runTest {
        val repository = SessionRepositoryImpl(database(Settings()), Store())
        assertFalse(repository.isActiveSession(null))
        assertTrue(repository.isActiveSession(Session("opaque", null, "u")))
        assertFalse(repository.isActiveSession(Session("opaque", null, null)))
    }

    @Test fun warmCacheLoadsTheStoredSession() = runTest {
        val repository = SessionRepositoryImpl(database(Settings()), Store(AuthResp(jwt("warm"), "r")))
        repository.warmCache()
        assertEquals("warm", repository.peekSession()?.userId)
    }
}
