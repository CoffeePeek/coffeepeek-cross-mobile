package com.coffeepeek.admin.di.auth

import com.coffeepeek.admin.session.UserSessionCleaner
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthAdaptersTest {
    @Test fun clearingLocalUserDataSignsTheSessionOutClearsTheOwnersFavoritesAndTheCaches() = runBlocking<Unit> {
        val log = mutableListOf<String>()
        val sessions = object : SessionRepository {
            private val state = MutableStateFlow<Session?>(Session("token", "refresh", "owner"))
            override fun peekSession() = state.value
            override fun applySession(session: Session?) { state.value = session }
            override fun isActiveSession(session: Session?) = session != null
            override suspend fun getSession() = state.value
            override suspend fun persistSession(session: Session?) = applySession(session)
            override suspend fun saveSession(session: Session?) { log += "save:${session?.userId}"; applySession(session) }
            override suspend fun warmCache() = Unit
            override fun observeSession() = state
            override suspend fun isLoggedIn() = state.value != null
        }
        val local = UserSessionLocalUserData(UserSessionCleaner(sessions, clearFavoritesCache = { log += "favorites" }, clearCaches = { log += "caches" }))
        local.clear()
        assertEquals(listOf("save:null", "favorites", "caches"), log)
        assertNull(sessions.peekSession())
    }
}
