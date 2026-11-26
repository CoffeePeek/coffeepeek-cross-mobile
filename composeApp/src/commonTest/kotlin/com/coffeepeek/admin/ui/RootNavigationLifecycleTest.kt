package com.coffeepeek.admin.ui

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class RootNavigationLifecycleTest {
    private val guest = RootNavigationSession(false, null, 0)
    private val first = RootNavigationSession(true, "first", 1)
    private val second = RootNavigationSession(true, "second", 2)

    @Test fun sameAccountLifetimeKeepsNavigationAndDismissedOnboarding() = runTest {
        val transitions = mutableListOf<RootSessionTransition>()
        val source = RootNavigationLifecycle(prepare = transitions::add)
        source.accept(guest)
        assertTrue(source.dismissOnboarding())
        source.accept(guest.copy())
        assertEquals(0, source.state.value.generation)
        assertTrue(source.state.value.onboardingDismissed)
        assertEquals(1, transitions.size)
    }

    @Test fun accountReplacementAndLogoutResetNavigationAndPrivateRequests() = runTest {
        val transitions = mutableListOf<RootSessionTransition>()
        val source = RootNavigationLifecycle(prepare = transitions::add)
        source.accept(first)
        source.accept(second)
        source.accept(guest.copy(generation = 3))
        assertEquals(2, source.state.value.generation)
        assertTrue(transitions.drop(1).all { it.resetsNavigation && it.clearsPrivateRequests })
        assertFalse(source.state.value.onboardingDismissed)
    }

    @Test fun loginResetsTheStackAndPreservesThePendingGuestHandoff() = runTest {
        val transitions = mutableListOf<RootSessionTransition>()
        val source = RootNavigationLifecycle(prepare = transitions::add)
        source.accept(guest)
        source.accept(first)
        assertTrue(transitions.last().resetsNavigation)
        assertFalse(transitions.last().clearsPrivateRequests)
        assertEquals(1, source.state.value.generation)
    }

    @Test fun onboardingCompletionIsOnceAndOnlyForAReadyGuestSession() = runTest {
        val source = RootNavigationLifecycle()
        assertFalse(source.dismissOnboarding())
        source.accept(guest)
        assertTrue(source.dismissOnboarding())
        assertFalse(source.dismissOnboarding())
        source.accept(first)
        assertFalse(source.state.value.onboardingDismissed)
        assertFalse(source.dismissOnboarding())
    }

    @Test fun oldNavigationIsPreparedBeforeNewStateAndHandoffsObserveNewState() = runTest {
        lateinit var source: RootNavigationLifecycle
        val sequence = mutableListOf<String>()
        source = RootNavigationLifecycle(prepare = { sequence.add("before:${source.state.value.session?.accountId}") },
            accepted = { sequence.add("after:${source.state.value.session?.accountId}") })
        source.accept(first)
        source.accept(second)
        assertEquals(listOf("before:null", "after:first", "before:first", "after:second"), sequence)
    }
}
