package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.auth.api.*
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class UsernameSelectionPickerTest {
    @Test fun guestFavoriteWaitsForUsernameBeforeStartingPickerExpiry() = runTest {
        val sessions = CheckInTestSessions(null)
        val gate = Gate()
        val requests = CollectionPickerRequests(sessions, backgroundScope, gate, testScheduler.timeSource::markNow)
        val target = CollectionTarget(CollectionTargetKind.Roaster, "roaster")
        assertFalse(requests.request(target))
        sessions.applySession(Session("token", userId = "owner"))
        runCurrent()
        advanceTimeBy(11 * 60_000)
        assertNull(requests.state.value)
        gate.ready(sessions.accountGeneration())
        runCurrent()
        val picker = assertNotNull(requests.state.value)
        assertEquals(target, picker.target)
        requests.presented(picker)
        advanceTimeBy(120_000)
        assertSame(picker, requests.state.value)
    }

    @Test fun abandoningSetupRevokesTheDeferredFavorite() = runTest {
        val sessions = CheckInTestSessions(null)
        val gate = Gate()
        val requests = CollectionPickerRequests(sessions, backgroundScope, gate)
        assertFalse(requests.request(CollectionTarget(CollectionTargetKind.CoffeeShop, "shop")))
        sessions.applySession(Session("A", userId = "owner"))
        runCurrent()
        sessions.applySession(null)
        runCurrent()
        sessions.applySession(Session("B", userId = "owner"))
        gate.ready(sessions.accountGeneration())
        runCurrent()
        assertNull(requests.state.value)
    }

    private class Gate : UsernameSelectionGate {
        override val state = MutableStateFlow(UsernameSelectionState(UsernameSelectionPhase.Required))
        override fun onAction(action: UsernameSelectionAction) = Unit
        fun ready(generation: Long) {
            state.value = UsernameSelectionState(UsernameSelectionPhase.Ready, "owner", generation)
        }
    }
}
