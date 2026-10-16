package com.coffeepeek.data.repository

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.data.session.SessionSecureStore
import com.coffeepeek.domain.model.Session
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SessionRepositoryConcurrencyTest {
    private class Store : SessionSecureStore {
        val state = MutableStateFlow<AuthResp?>(null)
        var readGate: CompletableDeferred<Unit>? = null
        var writeGate: CompletableDeferred<Unit>? = null
        override suspend fun read(): AuthResp? { val old = state.value; readGate?.await(); return old }
        override suspend fun write(data: AuthResp?) { writeGate?.await(); state.value = data }
        override fun observe() = state
    }
    private val database = object : DatabaseCore {
        override val settingRepository = object : SettingRepository {
            override suspend fun save(model: Setting) = Unit
            override suspend fun read(key: String): Setting? = null
            override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
            override suspend fun readAll() = emptyList<Setting>()
            override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
            override suspend fun delete(key: String) = Unit
        }
    }
    @Test fun delayedPersistenceCannotRestoreLoggedOutSession() = runTest {
        val store = Store()
        val repository = SessionRepositoryImpl(database, store)
        val old = Session("old", "refresh", "owner")
        repository.saveSession(old)
        repository.saveSession(null)
        repository.persistSession(old)
        assertNull(repository.getSession())
        assertNull(store.state.value)
        assertFalse(repository.replaceSession(old, old.copy(accessToken = "renewed")))
    }
    @Test fun coldReadCannotOverwriteNewerPublishedSession() = runTest {
        val store = Store().apply { state.value = AuthResp("old", "refresh"); readGate = CompletableDeferred() }
        val repository = SessionRepositoryImpl(database, store)
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
        val repository = SessionRepositoryImpl(database, store)
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
