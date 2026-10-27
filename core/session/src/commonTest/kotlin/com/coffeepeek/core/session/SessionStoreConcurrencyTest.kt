package com.coffeepeek.core.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SessionStoreConcurrencyTest {
    private class Store : SessionSecureStore {
        val state = MutableStateFlow<SessionCredentials?>(null)
        var readGate: CompletableDeferred<Unit>? = null
        var writeGate: CompletableDeferred<Unit>? = null
        override suspend fun read(): SessionCredentials? { val old = state.value; readGate?.await(); return old }
        override suspend fun write(data: SessionCredentials?) { writeGate?.await(); state.value = data }
        override fun observe() = state
    }
    private val legacyRows = object : LegacyCredentialRows {
        override suspend fun read(key: String): SessionCredentials? = null
        override suspend fun delete(key: String) = Unit
    }
    @Test fun delayedPersistenceCannotRestoreLoggedOutSession() = runTest {
        val store = Store()
        val repository = DefaultSessionStore(store, legacyRows)
        val old = Session("old", "refresh", "owner")
        repository.saveSession(old)
        repository.saveSession(null)
        repository.persistSession(old)
        assertNull(repository.getSession())
        assertNull(store.state.value)
        assertFalse(repository.replaceSession(old, old.copy(accessToken = "renewed")))
    }
    @Test fun coldReadCannotOverwriteNewerPublishedSession() = runTest {
        val store = Store().apply { state.value = SessionCredentials("old", "refresh"); readGate = CompletableDeferred() }
        val repository = DefaultSessionStore(store, legacyRows)
        val loading = async { repository.getSession() }
        runCurrent()
        val newer = Session("new", "next", "other")
        repository.applySession(newer)
        store.readGate!!.complete(Unit)
        assertEquals(newer, loading.await())
        repository.persistSession(newer)
        assertEquals("new", store.state.value?.accessToken)
    }
    @Test fun cancelledWriteCompletesBeforeLogoutCommits() = runTest {
        val store = Store().apply { writeGate = CompletableDeferred() }
        val repository = DefaultSessionStore(store, legacyRows)
        val saving = launch { repository.saveSession(Session("old", "refresh", "owner")) }
        runCurrent()
        saving.cancel()
        val logout = launch { repository.saveSession(null) }
        runCurrent()
        store.writeGate!!.complete(Unit)
        saving.join(); logout.join()
        assertNull(store.state.value)
        assertNull(repository.peekSession())
    }
}
