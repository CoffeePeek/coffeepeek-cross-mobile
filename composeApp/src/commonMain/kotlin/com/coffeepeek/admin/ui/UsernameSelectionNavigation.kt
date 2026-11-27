package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.api.UsernameSelectionEntry
import com.coffeepeek.feature.auth.api.UsernameSelectionGate
import com.coffeepeek.feature.auth.api.UsernameSelectionPhase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.koin.compose.koinInject

/** Credentials and product readiness are separate; no auth continuation consumes an unfinished account. */
internal fun SessionReader.observeReadyRootNavigationSession(gate: UsernameSelectionGate) =
    gate.state.map { setup ->
        val session = currentRootNavigationSession()
        session.takeIf { (setup.phase == UsernameSelectionPhase.SignedOut && !it.isLoggedIn) ||
            (it.isLoggedIn && setup.readyFor(it.accountId, it.generation)) }
    }.filterNotNull().distinctUntilChanged()

@Composable
internal fun UsernameSelectionNavigation(root: RootNavigationState, content: @Composable () -> Unit) {
    val gate = koinInject<UsernameSelectionGate>()
    val setup by gate.state.collectAsState()
    if (setup.blocksApp) koinInject<UsernameSelectionEntry>().Content(Modifier)
    else if (root.session == koinInject<SessionReader>().currentRootNavigationSession()) content()
}
