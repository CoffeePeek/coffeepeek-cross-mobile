package com.coffeepeek.admin.session

import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStoreCredentialsTest {
    private class Store(stored: Session?, private val loaded: Session? = stored) : SessionStore {
        var current: Session? = null
        var warmed = 0
        init { current = stored }
        override fun peekSession() = current
        override fun applySession(session: Session?) { current = session }
        override fun isActiveSession(session: Session?) = !session?.accessToken.isNullOrBlank()
        override fun observeSession(): Flow<Session?> = flowOf(current)
        override suspend fun getSession() = current
        override suspend fun persistSession(session: Session?) = Unit
        override suspend fun saveSession(session: Session?) { current = session }
        override suspend fun warmCache() { if (warmed++ == 0 && current == null) current = loaded }
        override suspend fun isLoggedIn() = isActiveSession(current)
    }

    @Test fun readsTheStoredSessionAfterWarmingTheCache() = runTest {
        val store = Store(stored = null, loaded = Session("stored-token", "stored-refresh", "one"))
        val tokens = SessionCredentialsOf(store).load()
        assertEquals(SessionTokens("stored-token", "stored-refresh"), tokens)
        assertEquals(1, store.warmed)
    }

    @Test fun anInactiveOrMissingSessionYieldsNoCredentials() = runTest {
        assertNull(SessionCredentialsOf(Store(null)).load())
        assertNull(SessionCredentialsOf(Store(Session("", "refresh", "one"))).load())
    }

    @Test fun followsTheSessionOfEveryRead() = runTest {
        val store = Store(Session("first", "r1", "one"))
        val credentials = SessionCredentialsOf(store)
        assertEquals("first", credentials.load()?.accessToken)
        store.applySession(Session("second", "r2", "two"))
        assertEquals(SessionTokens("second", "r2"), credentials.load())
        store.applySession(null)
        assertNull(credentials.load())
    }

    @Test fun savingWithoutARefreshTokenKeepsTheStoredOneAndTheUser() = runTest {
        val store = Store(Session("old", "stored-refresh", "owner"))
        SessionCredentialsOf(store).save(SessionTokens("new", ""))
        assertEquals(Session("new", "stored-refresh", "owner"), store.current)
    }

    @Test fun savingNothingSignsTheSessionOut() = runTest {
        val store = Store(Session("old", "refresh", "owner"))
        SessionCredentialsOf(store).save(null)
        assertNull(store.current)
    }

    @Test fun replacesOnlyTheCredentialsThatWereRead() = runTest {
        val store = Store(Session("current", "refresh", "owner"))
        val credentials = SessionCredentialsOf(store)
        assertFalse(credentials.replace(SessionTokens("stale", "refresh"), SessionTokens("next", "next-refresh")))
        assertEquals("current", store.current?.accessToken)
        assertTrue(credentials.replace(SessionTokens("current", "refresh"), SessionTokens("next", "next-refresh")))
        assertEquals(Session("next", "next-refresh", "owner"), store.current)
    }

    @Test fun aSessionWithoutARefreshTokenMatchesAnEmptyExpectation() = runTest {
        val store = Store(Session("current", null, "owner"))
        assertTrue(SessionCredentialsOf(store).replace(SessionTokens("current", null), null))
        assertNull(store.current)
    }

    private fun SessionCredentialsOf(store: SessionStore) = SessionStoreCredentials(store)
}
