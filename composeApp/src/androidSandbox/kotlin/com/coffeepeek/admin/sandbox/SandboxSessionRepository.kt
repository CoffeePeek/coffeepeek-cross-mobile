package com.coffeepeek.admin.sandbox

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** No credential store, JWT, account lookup or production identity. */
internal class SandboxSessionRepository : SessionRepository {
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
    private fun localSession() = Session("sandbox-local-only", "sandbox-local-only", "11111111-1111-4111-8111-111111111111")
}
