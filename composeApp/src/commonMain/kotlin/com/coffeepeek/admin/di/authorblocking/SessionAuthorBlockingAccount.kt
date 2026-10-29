package com.coffeepeek.admin.di.authorblocking

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Exposes the session as the opaque account identity author blocking needs. */
class SessionAuthorBlockingAccount(private val sessions: SessionReader) : AuthorBlockingAccount {
    override fun current(): String? = key(sessions.peekSession())
    override val changes: Flow<String?> = sessions.observeSession().map(::key)

    private fun key(session: Session?): String? =
        session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
}
