package com.coffeepeek.admin.di.notifications

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionNotificationAccountTest {
    private class Sessions : SessionRepository {
        val state = MutableStateFlow<Session?>(null)
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session != null && session == state.value
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) = applySession(session)
        override suspend fun saveSession(session: Session?) = applySession(session)
        override suspend fun warmCache() = Unit
        override fun observeSession() = state
        override suspend fun isLoggedIn() = state.value != null
    }

    @Test fun theAccountIsTheUserOfTheActiveSessionAndNullWhenSignedOut() = runBlocking<Unit> {
        val sessions = Sessions()
        val account = SessionNotificationAccount(sessions)
        assertNull(account.userId)
        assertNull(account.observeUserId().first())
        sessions.applySession(Session("token", "refresh", "alice"))
        assertEquals("alice", account.userId)
        assertEquals("alice", account.observeUserId().first())
        sessions.applySession(null)
        assertNull(account.userId)
    }
}
