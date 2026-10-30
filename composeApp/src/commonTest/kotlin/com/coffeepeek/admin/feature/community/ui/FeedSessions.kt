package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow

/** An in-memory session for adapter tests; it does not touch storage. */
internal class FeedSessions(initial: Session?) : SessionStore {
    private val session = MutableStateFlow(initial)
    override fun peekSession() = session.value
    override fun applySession(session: Session?) { this.session.value = session }
    override fun isActiveSession(session: Session?) = !session?.accessToken.isNullOrBlank()
    override suspend fun getSession() = session.value
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override fun observeSession() = session
    override suspend fun isLoggedIn() = isActiveSession(session.value)
}
