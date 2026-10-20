package com.coffeepeek.feature.notifications.api

import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.notifications.data.HttpNotificationRepository
import com.coffeepeek.feature.notifications.domain.NotificationPreferences
import com.coffeepeek.feature.notifications.ui.NotificationPresenter
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.StateFlow

data class NotificationSettingsState(
    val signedIn: Boolean = false, val loading: Boolean = false,
    val preferences: NotificationPreferences? = null, val error: String? = null,
)

interface NotificationFeature {
    val state: StateFlow<NotificationSettingsState>
    val device: StateFlow<PushDeviceState>
    fun foreground()
    fun refresh()
    fun save(value: NotificationPreferences)
    fun requestPermission()
    suspend fun beforeLogout()
}

fun createNotificationFeature(client: HttpClient, sessions: SessionRepository, platform: PushDevicePlatform): NotificationFeature =
    NotificationPresenter(HttpNotificationRepository(client), sessions, platform)
