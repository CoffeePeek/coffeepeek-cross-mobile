package com.coffeepeek.admin.di.leaderboard

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi

private class LeaderboardSessions : SessionReader {
    val current = MutableStateFlow<Session?>(null)
    override fun peekSession() = current.value
    override suspend fun getSession() = current.value
    override fun observeSession() = current
    override fun isActiveSession(session: Session?) = session != null
}

private class LeaderboardAuthors : AuthorVisibility {
    override val visibility = MutableStateFlow(AuthorVisibilitySnapshot())
    override suspend fun refresh() = Result.success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardContextTest {
    private fun TestScope.observe(sessions: LeaderboardSessions, authors: LeaderboardAuthors): List<Boolean> =
        mutableListOf<Boolean>().also { values ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                leaderboardContextChanges(sessions, authors).collect { values += it }
            }
        }

    @Test fun tokenRefreshKeepsContextButAccountChangesInvalidateIt() = runTest {
        val sessions = LeaderboardSessions()
        val changes = observe(sessions, LeaderboardAuthors())
        runCurrent()
        sessions.current.value = Session("token-a", userId = "a")
        runCurrent()
        sessions.current.value = Session("refreshed-token-a", userId = "a")
        runCurrent()
        assertEquals(2, changes.size)
        sessions.current.value = Session("token-b", userId = "b")
        runCurrent()
        sessions.current.value = null
        runCurrent()
        assertEquals(listOf(true, true, true, true), changes)
    }

    @Test fun blockRevisionsInvalidateEvenWhileReadinessRemainsTrue() = runTest {
        val authors = LeaderboardAuthors()
        val changes = observe(LeaderboardSessions(), authors)
        runCurrent()
        authors.visibility.value = AuthorVisibilitySnapshot(revision = 1, blockedIds = setOf("a"))
        runCurrent()
        authors.visibility.value = AuthorVisibilitySnapshot(revision = 2, ready = false)
        runCurrent()
        authors.visibility.value = AuthorVisibilitySnapshot(revision = 3, ready = true)
        runCurrent()
        assertEquals(listOf(true, true, false, true), changes)
    }
}
