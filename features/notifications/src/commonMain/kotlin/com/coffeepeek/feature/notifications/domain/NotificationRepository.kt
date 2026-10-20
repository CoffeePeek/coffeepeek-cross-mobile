package com.coffeepeek.feature.notifications.domain

internal interface NotificationRepository {
    suspend fun save(value: NotificationPreferences)
    suspend fun register(installationId: String, token: String)
    suspend fun remove(installationId: String)
    suspend fun activity()
}
