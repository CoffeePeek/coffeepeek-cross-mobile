package com.coffeepeek.admin.di.authorblocking

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Exposes the legacy session as the opaque account identity author blocking needs. */
class SessionAuthorBlockingAccount(private val sessions: SessionRepository) : AuthorBlockingAccount {
    override fun current(): String? = key(sessions.peekSession())
    override val changes: Flow<String?> = sessions.observeSession().map(::key)

    private fun key(session: Session?): String? =
        session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
}
