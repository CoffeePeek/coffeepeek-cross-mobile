package com.coffeepeek.admin.session

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.flow.MutableStateFlow

/** Deliberately has no legacy or writer interface: app adapters receive reads only. */
internal open class ReadOnlySessions(initial: Session? = null) : SessionReader {
    val state = MutableStateFlow(initial)
    var active = true
    override fun peekSession() = state.value
    override fun isActiveSession(session: Session?) = active && session != null && session == state.value
    override fun observeSession(): kotlinx.coroutines.flow.Flow<Session?> = state
    override suspend fun getSession() = state.value
}
