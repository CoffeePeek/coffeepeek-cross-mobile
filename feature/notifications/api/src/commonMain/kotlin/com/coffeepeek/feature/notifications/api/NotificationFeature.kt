package com.coffeepeek.feature.notifications.api

import kotlinx.coroutines.flow.StateFlow

interface NotificationFeature {
    val state: StateFlow<NotificationInboxState>
    fun refresh()
    fun loadMore()
    fun setPushEnabled(enabled: Boolean)
    fun markRead(id: String)
    fun markAllRead()
    fun foreground()
    /** Called only after a new check-in has been successfully saved. */
    fun onCheckInCreated()
    suspend fun beforeLogout()
}
