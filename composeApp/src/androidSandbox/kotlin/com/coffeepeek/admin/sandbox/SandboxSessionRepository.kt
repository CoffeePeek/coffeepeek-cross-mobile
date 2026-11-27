package com.coffeepeek.admin.sandbox

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * No credential store, account lookup or production identity. The access token is an unsigned local JWT that carries only
 * a fake username and an expiry in 2100: the protected photo loader (check-in photos, shelf cards) needs `exp` to bound how long it shows a photo.
 */
internal class SandboxSessionRepository : SessionStore {
    private val current = MutableStateFlow<Session?>(localSession())
    fun login() = applySession(localSession())
    override fun peekSession() = current.value
    override fun applySession(session: Session?) { current.value = session }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = peekSession()
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun replaceSession(expected: Session?, session: Session?) = current.compareAndSet(expected, session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override fun observeSession() = current.asStateFlow()
    override suspend fun isLoggedIn() = current.value != null
    private fun localSession() = Session(LOCAL_TOKEN, "sandbox-local-only", "11111111-1111-4111-8111-111111111111")

    private companion object {
        /** Unsigned local-only fixture; never accepted by the real API. */
        const val LOCAL_TOKEN = "eyJhbGciOiJub25lIn0.eyJleHAiOjQxMDI0NDQ4MDAsIm5hbWUiOiJTYW5kYm94Iiwic3ViIjoiMTExMTExMTEtMTExMS00MTExLTgxMTEtMTExMTExMTExMTExIn0."
    }
}
