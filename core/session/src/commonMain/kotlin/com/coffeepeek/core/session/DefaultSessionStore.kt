package com.coffeepeek.core.session

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

/** In-memory snapshot with serialized persistence and a one-time import of pre-secure-store rows. */
class DefaultSessionStore(
    private val secureStore: SessionSecureStore,
    private val legacyRows: LegacyCredentialRows,
) : SessionStore {
    private data class Snapshot(val revision: Long = 0, val initialized: Boolean = false, val session: Session? = null)
    private val state = MutableStateFlow(Snapshot())
    private val writer = Mutex()
    override fun peekSession(): Session? = state.value.session
    override fun applySession(session: Session?) {
        state.update { Snapshot(it.revision + 1, true, session) }
    }
    override fun isActiveSession(session: Session?) = SessionAuth.isActive(session)
    override suspend fun warmCache() { getSession() }
    override suspend fun getSession(): Session? = writer.withLock {
        val initial = state.value
        if (!initial.initialized) {
            val stored = secureStore.read() ?: (legacyRows.read(LegacyCredentialRows.SESSION)
                ?: legacyRows.read(LegacyCredentialRows.API_KEYS))?.also {
                withContext(NonCancellable) {
                    secureStore.write(it)
                    legacyRows.delete(LegacyCredentialRows.SESSION)
                    legacyRows.delete(LegacyCredentialRows.API_KEYS)
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
            // A signed-out state must not be undone by an old settings row on the next start: clear them first, so a
            // failure here leaves the secure store as it was instead of an empty one that would import the old row.
            if (snapshot.session == null) {
                legacyRows.delete(LegacyCredentialRows.SESSION)
                legacyRows.delete(LegacyCredentialRows.API_KEYS)
            }
            secureStore.write(snapshot.session?.toCredentials())
            if (state.value == snapshot) break
        }
    }
    override fun observeSession(): Flow<Session?> = flow {
        getSession()
        emitAll(state.map { it.session }.distinctUntilChanged())
    }
    override suspend fun isLoggedIn() = isActiveSession(getSession())
    private fun SessionCredentials.toSession() = Session(accessToken, refreshToken, JwtUtils.extractUserId(accessToken))
    private fun Session.toCredentials() = SessionCredentials(accessToken, refreshToken.orEmpty())
}
