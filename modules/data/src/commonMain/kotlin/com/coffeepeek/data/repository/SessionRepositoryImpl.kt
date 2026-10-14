package com.coffeepeek.data.repository

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.data.util.JwtUtils
import com.coffeepeek.data.util.SessionAuth
import com.coffeepeek.data.session.SessionSecureStore
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.repository.readSerializable
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SessionRepositoryImpl(database: DatabaseCore, private val secureStore: SessionSecureStore) : SessionRepository {
    private data class Snapshot(val revision: Long = 0, val initialized: Boolean = false, val session: Session? = null)
    private val state = MutableStateFlow(Snapshot())
    private val writer = Mutex()
    private val settings = database.settingRepository
    override fun peekSession(): Session? = state.value.session
    override fun applySession(session: Session?) {
        state.update { Snapshot(it.revision + 1, true, session) }
    }
    override fun isActiveSession(session: Session?) = SessionAuth.isActive(session)
    override suspend fun warmCache() { getSession() }
    override suspend fun getSession(): Session? = writer.withLock {
        val initial = state.value
        if (!initial.initialized) {
            val stored = secureStore.read() ?: (settings.readSerializable<AuthResp>("SESSION")
                ?: settings.readSerializable<AuthResp>("API_KEYS"))?.also {
                withContext(NonCancellable) {
                    secureStore.write(it)
                    settings.delete("SESSION")
                    settings.delete("API_KEYS")
                }
            }
            state.compareAndSet(initial, initial.copy(initialized = true, session = stored?.toSession()))
        }
        state.value.session
    }
    override suspend fun persistSession(session: Session?) {
        val expected = state.value
        if (expected.session != session) return
        writer.withLock {
            if (state.value == expected) persistLatest()
        }
    }
    override suspend fun saveSession(session: Session?) = writer.withLock {
        applySession(session)
        persistLatest()
    }
    override suspend fun replaceSession(expected: Session?, session: Session?): Boolean = writer.withLock {
        if (state.value.session != expected) return@withLock false
        applySession(session)
        persistLatest()
        true
    }
    // Commit published state despite request cancellation; never restore older storage emissions.
    private suspend fun persistLatest(): Unit = withContext(NonCancellable) {
        while (true) {
            val snapshot = state.value
            secureStore.write(snapshot.session?.toAuthResp())
            if (state.value == snapshot) break
        }
    }
    override fun observeSession(): Flow<Session?> = flow {
        getSession()
        emitAll(state.map { it.session }.distinctUntilChanged())
    }
    override suspend fun isLoggedIn() = isActiveSession(getSession())
    private fun AuthResp.toSession() = Session(accessToken, refreshToken, JwtUtils.extractUserId(accessToken))
    private fun Session.toAuthResp() = AuthResp(accessToken, refreshToken.orEmpty())
}
