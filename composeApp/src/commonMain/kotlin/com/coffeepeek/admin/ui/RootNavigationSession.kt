package com.coffeepeek.admin.ui

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.session.Session
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Root controllers belong to an account lifetime, not to a credential or a login Boolean. */
internal data class RootNavigationSession(val isLoggedIn: Boolean, val accountId: String?, val generation: Long)

internal fun SessionReader.currentRootNavigationSession(session: Session? = peekSession()): RootNavigationSession {
    val loggedIn = isActiveSession(session)
    return RootNavigationSession(loggedIn, session?.userId.takeIf { loggedIn }, accountGeneration())
}

internal fun SessionReader.observeRootNavigationSession() = observeSession()
    .map { currentRootNavigationSession(it) }.distinctUntilChanged()
