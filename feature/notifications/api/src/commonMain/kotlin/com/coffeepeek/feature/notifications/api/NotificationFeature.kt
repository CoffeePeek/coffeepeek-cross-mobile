package com.coffeepeek.feature.notifications.api

import kotlinx.coroutines.flow.StateFlow

interface NotificationFeature {
    val state: StateFlow<NotificationInboxState>
    fun refresh()
    fun loadMore()
    fun setPushEnabled(enabled: Boolean)
    fun markRead(id: String)
    fun markAllRead()
    /** Resolves current owned metadata; null means there is no supported destination. */
    suspend fun destination(notificationId: String): NotificationDestination?
    fun foreground()
    /** Called only after a new check-in has been successfully saved. */
    fun onCheckInCreated()
    /** Explicit user opening of the inbox; shares the successful check-in permission opportunity. */
    fun onInboxOpened()
    suspend fun beforeLogout()
}
