package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.session.ReadOnlySessions
import com.coffeepeek.core.session.Session
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsMediaScopeTest {
    @Test fun tokenRotationRetainsMediaButANewLoginAndLogoutReplaceItsScope() = runTest {
        val sessions = MediaSessions()
        val account = SessionCollectionsAccount(sessions)
        val scopes = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { account.mediaScopes.toList(scopes) }
        val original = account.mediaScope()
        sessions.state.value = sessions.state.value?.copy(accessToken = "rotated-token")
        assertEquals(listOf(original), scopes)
        sessions.generation++
        sessions.state.value = sessions.state.value?.copy(accessToken = "new-login-token")
        assertNotEquals(original, account.mediaScope())
        assertEquals(listOf(original, account.mediaScope()), scopes)
        sessions.state.value = null
        assertNull(account.mediaScope())
        assertEquals(3, scopes.size)
        assertNull(scopes.last())
    }
}

private class MediaSessions : ReadOnlySessions(Session("fixture-token", userId = "owner")) {
    var generation = 1L
    override fun accountGeneration() = generation
}
