package com.coffeepeek.admin.ui

import com.coffeepeek.core.session.DefaultSessionStore
import com.coffeepeek.core.session.LegacyCredentialRows
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionCredentials
import com.coffeepeek.core.session.SessionSecureStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RootNavigationSessionTest {
    @Test fun replacingASignedInAccountChangesTheNativeRootLifetime() = runTest {
        val sessions = sessions().apply { applySession(account("first")) }
        val observed = mutableListOf<RootNavigationSession>()
        backgroundScope.launch { sessions.observeRootNavigationSession().collect { observed += it } }
        runCurrent()
        sessions.applySession(account("second"))
        runCurrent()
        assertEquals(listOf("first", "second"), observed.map { it.accountId })
        assertTrue(observed.all { it.isLoggedIn })
        assertNotEquals(observed.first().generation, observed.last().generation)
    }

    @Test fun conflatedSignOutAndSignInStillReplaceTheSameAccountsNativeRoot() = runTest {
        val original = account("owner")
        val sessions = sessions().apply { applySession(original) }
        val observed = mutableListOf<RootNavigationSession>()
        backgroundScope.launch { sessions.observeRootNavigationSession().collect { observed += it } }
        runCurrent()
        sessions.applySession(null)
        sessions.applySession(original)
        runCurrent()
        assertEquals(2, observed.size)
        assertTrue(observed.all { it.isLoggedIn && it.accountId == "owner" })
        assertNotEquals(observed.first().generation, observed.last().generation)
    }

    @Test fun credentialRenewalKeepsTheRootAndItsNavigationStack() = runTest {
        val original = account("owner")
        val sessions = sessions().apply { applySession(original) }
        val observed = mutableListOf<RootNavigationSession>()
        backgroundScope.launch { sessions.observeRootNavigationSession().collect { observed += it } }
        runCurrent()
        sessions.applySession(original.copy(accessToken = "renewed", refreshToken = "renewed-refresh"))
        runCurrent()
        assertEquals(1, observed.size)
        assertTrue(observed.single().isLoggedIn)
    }

    @Test fun guestAndSignedInRootsRemainDistinct() = runTest {
        val sessions = sessions()
        val observed = mutableListOf<RootNavigationSession>()
        backgroundScope.launch { sessions.observeRootNavigationSession().collect { observed += it } }
        runCurrent()
        sessions.applySession(account("owner"))
        runCurrent()
        assertFalse(observed.first().isLoggedIn)
        assertEquals(null, observed.first().accountId)
        assertTrue(observed.last().isLoggedIn)
    }

    private fun account(id: String) = Session("fixture-access", "fixture-refresh", id)

    private fun sessions() = DefaultSessionStore(
        object : SessionSecureStore {
            override suspend fun read(): SessionCredentials? = null
            override suspend fun write(data: SessionCredentials?) = Unit
            override fun observe() = flowOf<SessionCredentials?>(null)
        },
        object : LegacyCredentialRows {
            override suspend fun read(key: String): SessionCredentials? = null
            override suspend fun delete(key: String) = Unit
        },
    )
}
