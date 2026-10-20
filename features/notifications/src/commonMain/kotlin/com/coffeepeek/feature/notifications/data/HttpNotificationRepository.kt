package com.coffeepeek.feature.notifications.data

import com.coffeepeek.feature.notifications.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.Serializable

internal class HttpNotificationRepository(private val client: HttpClient) : NotificationRepository {
    private val root = "/api/v1/me/notifications"
    override suspend fun save(value: NotificationPreferences) {
        client.put("$root/preferences") { expectSuccess = true; contentType(ContentType.Application.Json)
            setBody(PreferencesDto(value.moderation, value.inactivity, value.custom)) }.checked()
    }
    override suspend fun register(installationId: String, token: String) {
        client.put("$root/devices/$installationId") { expectSuccess = true; contentType(ContentType.Application.Json)
            setBody(DeviceDto(token)) }.checked()
    }
    override suspend fun remove(installationId: String) { client.delete("$root/devices/$installationId") { expectSuccess = true }.checked() }
    override suspend fun activity() { client.post("$root/activity") { expectSuccess = true }.checked() }
    private fun HttpResponse.checked() { check(status.isSuccess()) { "Notification request failed" } }
}

@Serializable private data class PreferencesDto(val moderation: Boolean, val inactivity: Boolean, val custom: Boolean)
@Serializable private data class DeviceDto(val token: String)
