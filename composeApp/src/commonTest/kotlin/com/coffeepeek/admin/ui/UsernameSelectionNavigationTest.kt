package com.coffeepeek.admin.ui

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.auth.api.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class UsernameSelectionNavigationTest {
    @Test fun rootCannotConsumeAuthenticationHandoffBeforeProfileAndCredentialsAreReady() = runTest {
        val sessions = CheckInTestSessions(null)
        val gate = Gate()
        val roots = mutableListOf<RootNavigationSession>()
        backgroundScope.launch { sessions.observeReadyRootNavigationSession(gate).collect { roots += it } }
        runCurrent()
        sessions.applySession(Session("pending", userId = "owner"))
        runCurrent()
        assertTrue(roots.none { it.isLoggedIn })
        gate.state.value = UsernameSelectionState(UsernameSelectionPhase.Ready, "owner", sessions.accountGeneration())
        runCurrent()
        assertEquals(1, roots.count { it.isLoggedIn })
        sessions.applySession(Session("refreshed", userId = "owner"))
        runCurrent()
        assertEquals(1, roots.count { it.isLoggedIn })
    }
    private class Gate : UsernameSelectionGate {
        override val state = MutableStateFlow(UsernameSelectionState(UsernameSelectionPhase.SignedOut))
        override fun onAction(action: UsernameSelectionAction) = Unit
    }
}
