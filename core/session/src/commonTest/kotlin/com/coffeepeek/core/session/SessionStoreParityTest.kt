package com.coffeepeek.core.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class SessionStoreParityTest {
    private class Store(initial: SessionCredentials? = null) : SessionSecureStore {
        val state = MutableStateFlow(initial)
        var writes = 0
        override suspend fun read(): SessionCredentials? = state.value
        override suspend fun write(data: SessionCredentials?) { writes++; state.value = data }
        override fun observe() = state
    }
    private class Rows(vararg initial: Pair<String, SessionCredentials>) : LegacyCredentialRows {
        val rows = initial.toMap().toMutableMap()
        override suspend fun read(key: String): SessionCredentials? = rows[key]
        override suspend fun delete(key: String) { rows.remove(key) }
    }
    private fun jwt(sub: String) = "h." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        .encode("""{"sub":"$sub"}""".encodeToByteArray()) + ".s"

    @Test fun storedCredentialsBecomeASessionWithTheUserIdFromTheToken() = runTest {
        val token = jwt("owner")
        val repository = DefaultSessionStore(Store(SessionCredentials(token, "refresh")), Rows())
        assertNull(repository.peekSession())
        assertEquals(Session(token, "refresh", "owner"), repository.getSession())
        assertEquals(Session(token, "refresh", "owner"), repository.peekSession())
        assertTrue(repository.isLoggedIn())
    }

    @Test fun nothingStoredMeansSignedOut() = runTest {
        val repository = DefaultSessionStore(Store(), Rows())
        assertNull(repository.getSession())
        assertFalse(repository.isLoggedIn())
    }

    @Test fun oldSettingsRowsAreImportedOnceAndDeleted() = runTest {
        val token = jwt("legacy")
        val rows = Rows("SESSION" to SessionCredentials(token, "r1"), "API_KEYS" to SessionCredentials("older", "r0"))
        val store = Store()
        val repository = DefaultSessionStore(store, rows)
        assertEquals(Session(token, "r1", "legacy"), repository.getSession())
        assertEquals(SessionCredentials(token, "r1"), store.state.value)
        assertTrue(rows.rows.isEmpty())
        assertEquals(1, store.writes)
        repository.getSession()
        assertEquals(1, store.writes)
    }

    @Test fun apiKeysRowIsTheFallbackWhenThereIsNoSessionRow() = runTest {
        val rows = Rows("API_KEYS" to SessionCredentials(jwt("keys"), "r"))
        val repository = DefaultSessionStore(Store(), rows)
        assertEquals("keys", repository.getSession()?.userId)
        assertTrue(rows.rows.isEmpty())
    }

    @Test fun secureStoreWinsAndLeavesOldRowsAlone() = runTest {
        val rows = Rows("SESSION" to SessionCredentials(jwt("old"), "r"))
        val repository = DefaultSessionStore(Store(SessionCredentials(jwt("current"), "r")), rows)
        assertEquals("current", repository.getSession()?.userId)
        assertTrue("SESSION" in rows.rows)
    }

    @Test fun saveWritesTheSecureStoreWhileApplyOnlyChangesMemory() = runTest {
        val store = Store()
        val repository = DefaultSessionStore(store, Rows())
        val first = Session("a", null, "u")
        repository.applySession(first)
        assertEquals(first, repository.peekSession())
        assertNull(store.state.value)
        repository.persistSession(first)
        assertEquals(SessionCredentials("a", ""), store.state.value)
        val second = Session("b", "r", "u")
        repository.saveSession(second)
        assertEquals(SessionCredentials("b", "r"), store.state.value)
        repository.saveSession(null)
        assertNull(store.state.value)
        assertNull(repository.peekSession())
    }

    @Test fun replaceSwapsOnlyWhileTheExpectedSessionIsCurrent() = runTest {
        val store = Store()
        val repository = DefaultSessionStore(store, Rows())
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
        val repository = DefaultSessionStore(Store(), Rows())
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
        val repository = DefaultSessionStore(Store(), Rows())
        assertFalse(repository.isActiveSession(null))
        assertTrue(repository.isActiveSession(Session("opaque", null, "u")))
        assertFalse(repository.isActiveSession(Session("opaque", null, null)))
    }

    @Test fun warmCacheLoadsTheStoredSession() = runTest {
        val repository = DefaultSessionStore(Store(SessionCredentials(jwt("warm"), "r")), Rows())
        repository.warmCache()
        assertEquals("warm", repository.peekSession()?.userId)
    }
}
