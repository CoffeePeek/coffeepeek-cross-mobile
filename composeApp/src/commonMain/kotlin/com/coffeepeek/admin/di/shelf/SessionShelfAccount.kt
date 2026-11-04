package com.coffeepeek.admin.di.shelf

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shelf.data.repository.ShelfAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The shelf owner is the active session's user; an expired or replaced session is nobody's shelf. */
internal class SessionShelfAccount(private val sessions: SessionReader) : ShelfAccount {
    override val owner: String?
        get() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)

    override val visibilityScope: String?
        get() = owner?.let { "$it:${sessions.accountGeneration()}" }

    override val owners: Flow<String?>
        get() = sessions.observeSession()
            .map { session -> session?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank) }
            .distinctUntilChanged()

    override val visibilityScopes: Flow<String?>
        get() = sessions.observeSession()
            .map { session -> session?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
                ?.let { "$it:${sessions.accountGeneration()}" } }
            .distinctUntilChanged()
}
