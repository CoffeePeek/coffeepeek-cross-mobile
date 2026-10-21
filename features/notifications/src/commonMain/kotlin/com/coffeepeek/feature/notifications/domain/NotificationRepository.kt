package com.coffeepeek.feature.notifications.domain

internal interface NotificationRepository {
    suspend fun inbox(before: String? = null): NotificationPage
    suspend fun pushEnabled(): Boolean
    suspend fun setPushEnabled(enabled: Boolean)
    suspend fun markRead(id: String)
    suspend fun markAllRead(throughId: String)
    suspend fun register(installationId: String, token: String)
    suspend fun remove(installationId: String)
    suspend fun activity()
}
