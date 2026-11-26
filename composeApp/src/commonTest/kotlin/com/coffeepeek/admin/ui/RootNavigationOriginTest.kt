package com.coffeepeek.admin.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class RootNavigationOriginTest {
    private val first = RootNavigationSession(true, "first", 1)

    @Test fun queuedCompletionIsAcceptedByItsOriginalRootAndSession() = runTest {
        val origin = RootNavigationOrigin(first)
        val event = Navigator.NavEvent.ReturnToProfile(origin)
        var delivered = false
        launch { delivered = origin.accepts(event, first) }
        runCurrent()
        assertTrue(delivered)
    }

    @Test fun queuedCompletionCannotNavigateAReplacementAccountRoot() = runTest {
        val event = Navigator.NavEvent.ReturnToProfile(RootNavigationOrigin(first))
        var root = event.origin
        var session = first
        var delivered = false
        launch { delivered = root.accepts(event, session) }
        session = RootNavigationSession(true, "second", 2)
        root = RootNavigationOrigin(session)
        runCurrent()
        assertFalse(delivered)
    }

    @Test fun queuedCompletionCannotNavigateAReplacementRootForTheSameSession() = runTest {
        val event = Navigator.NavEvent.ReturnToProfile(RootNavigationOrigin(first))
        var root = event.origin
        var delivered = false
        launch { delivered = root.accepts(event, first) }
        root = RootNavigationOrigin(first)
        runCurrent()
        assertFalse(delivered)
    }

    @Test fun queuedCompletionRejectsALifetimeChangeBeforeTheRootCollectorResumes() = runTest {
        val origin = RootNavigationOrigin(first)
        val event = Navigator.NavEvent.ReturnToProfile(origin)
        var session = first
        var delivered = false
        launch { delivered = origin.accepts(event, session) }
        session = RootNavigationSession(true, "second", 2)
        session = first.copy(generation = 3)
        runCurrent()
        assertFalse(delivered)
    }
}
