package com.coffeepeek.admin.di.notifications

import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.notifications.api.NotificationAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Exposes the user id of the active legacy session as the account the notification inbox belongs to. */
class SessionNotificationAccount(private val sessions: SessionRepository) : NotificationAccount {
    override val userId: String? get() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId
    override fun observeUserId(): Flow<String?> = sessions.observeSession().map { it?.takeIf(sessions::isActiveSession)?.userId }
}
