package com.coffeepeek.feature.notifications.api

import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.notifications.data.HttpNotificationRepository
import com.coffeepeek.feature.notifications.ui.NotificationPresenter
import io.ktor.client.HttpClient

interface NotificationFeature {
    fun foreground()
    /** Called only after a new check-in has been successfully saved. */
    fun onCheckInCreated()
    suspend fun beforeLogout()
}

fun createNotificationFeature(client: HttpClient, sessions: SessionRepository, platform: PushDevicePlatform): NotificationFeature =
    NotificationPresenter(HttpNotificationRepository(client), sessions, platform)
