package com.coffeepeek.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A logout must survive a restart: old settings rows may never bring a signed-out session back. */
class SessionLogoutDurabilityTest {
    private class Store(initial: SessionCredentials? = null) : SessionSecureStore {
        val state = MutableStateFlow(initial)
        override suspend fun read(): SessionCredentials? = state.value
        override suspend fun write(data: SessionCredentials?) { state.value = data }
        override fun observe() = state
    }
    private class Rows(vararg initial: Pair<String, SessionCredentials>) : LegacyCredentialRows {
        val rows = initial.toMap().toMutableMap()
        var failDeletes = false
        override suspend fun read(key: String): SessionCredentials? = rows[key]
        override suspend fun delete(key: String) { if (failDeletes) error("settings are locked"); rows.remove(key) }
    }
    private val current = SessionCredentials("current", "r")
    private val stale = SessionCredentials("stale", "r0")
    private fun coexisting() = Rows(LegacyCredentialRows.SESSION to stale, LegacyCredentialRows.API_KEYS to stale)

    @Test fun logoutRemovesOldRowsSoARestartStaysSignedOut() = runTest {
        val secure = Store(current)
        val rows = coexisting()
        val first = DefaultSessionStore(secure, rows)
        assertEquals("current", first.getSession()?.accessToken)
        assertEquals(2, rows.rows.size)

        first.saveSession(null)

        assertTrue(rows.rows.isEmpty())
        assertNull(secure.state.value)
        assertNull(DefaultSessionStore(secure, rows).getSession())
    }

    @Test fun persistingASignedOutStateAlsoClearsTheOldRows() = runTest {
        val secure = Store(current)
        val rows = coexisting()
        val store = DefaultSessionStore(secure, rows)
        store.getSession()
        store.applySession(null)
        store.persistSession(null)
        assertTrue(rows.rows.isEmpty())
        assertNull(DefaultSessionStore(secure, rows).getSession())
    }

    @Test fun signingInAgainAfterLogoutIsRestoredByTheNextStart() = runTest {
        val secure = Store(current)
        val rows = coexisting()
        val store = DefaultSessionStore(secure, rows)
        store.getSession()
        store.saveSession(null)
        store.saveSession(Session("fresh", "r2", "u"))
        assertEquals("fresh", DefaultSessionStore(secure, rows).getSession()?.accessToken)
    }

    @Test fun aFailureToClearTheOldRowsIsReportedAndLeavesTheSecureStoreUntouched() = runTest {
        val secure = Store(current)
        val rows = coexisting().also { it.failDeletes = true }
        val store = DefaultSessionStore(secure, rows)
        store.getSession()
        assertFailsWith<IllegalStateException> { store.saveSession(null) }
        // No half logout: the secure store still holds the account, so a restart cannot import a different one.
        assertNotNull(secure.state.value)
        assertEquals(2, rows.rows.size)
    }

    @Test fun anOrdinaryFirstImportStillMovesTheOldRowIntoTheSecureStore() = runTest {
        val secure = Store()
        val rows = Rows(LegacyCredentialRows.SESSION to current)
        assertEquals("current", DefaultSessionStore(secure, rows).getSession()?.accessToken)
        assertEquals(current, secure.state.value)
        assertTrue(rows.rows.isEmpty())
    }
}
