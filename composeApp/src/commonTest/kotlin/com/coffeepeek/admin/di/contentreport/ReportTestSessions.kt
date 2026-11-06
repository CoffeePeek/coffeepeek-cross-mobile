package com.coffeepeek.admin.di.contentreport

import com.coffeepeek.core.session.*
import kotlinx.coroutines.flow.*

internal class ReportTestSessions(initial: Session? = Session("token-a", userId = "owner")) : SessionReader {
    val current = MutableStateFlow(initial)
    var source: Flow<Session?> = current
    override fun peekSession() = current.value
    override fun isActiveSession(session: Session?) = session != null && session.accessToken.isNotBlank() && session.accessToken != "expired"
    override fun observeSession() = source
    override suspend fun getSession() = current.value
}
