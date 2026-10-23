package com.coffeepeek.feature.notifications.data

import com.coffeepeek.feature.notifications.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.Serializable

/** The transport of the notification history, push settings and device registration. */
fun createNotificationRepository(client: HttpClient): NotificationRepository = HttpNotificationRepository(client)

internal class HttpNotificationRepository(private val client: HttpClient) : NotificationRepository {
    private val root = "/api/v1/me/notifications"
    override suspend fun inbox(before: String?): NotificationPage {
        val page = client.get(root) {
            expectSuccess = true
            parameter("pageSize", 30)
            before?.let { parameter("before", it) }
        }.body<InboxDto>()
        return NotificationPage(page.items.map {
            Notification(it.id, it.type, it.title, it.body, it.createdAtUtc, it.readAtUtc, it.status)
        }, page.nextCursor, page.unreadCount)
    }
    override suspend fun pushEnabled() = client.get("$root/push") { expectSuccess = true }.body<PushSettingsDto>().enabled
    override suspend fun setPushEnabled(enabled: Boolean) {
        client.put("$root/push") {
            expectSuccess = true; contentType(ContentType.Application.Json); setBody(PushSettingsDto(enabled))
        }.checked()
    }
    override suspend fun markRead(id: String) {
        client.put("$root/$id/read") { expectSuccess = true }.checked()
    }
    override suspend fun markAllRead(throughId: String) {
        client.put("$root/read") {
            expectSuccess = true; contentType(ContentType.Application.Json); setBody(ReadAllDto(throughId))
        }.checked()
    }
    override suspend fun register(installationId: String, token: String) {
        client.put("$root/devices/$installationId") { expectSuccess = true; contentType(ContentType.Application.Json)
            setBody(DeviceDto(token)) }.checked()
    }
    override suspend fun remove(installationId: String) { client.delete("$root/devices/$installationId") { expectSuccess = true }.checked() }
    override suspend fun activity() { client.post("$root/activity") { expectSuccess = true }.checked() }
    private fun HttpResponse.checked() { check(status.isSuccess()) { "Notification request failed" } }
}

@Serializable private data class DeviceDto(val token: String)
@Serializable private data class PushSettingsDto(val enabled: Boolean)
@Serializable private data class ReadAllDto(val throughId: String)
@Serializable private data class InboxDto(val items: List<NotificationDto>, val nextCursor: String?, val unreadCount: Int)
@Serializable private data class NotificationDto(
    val id: String, val type: String, val title: String, val body: String,
    val createdAtUtc: String, val readAtUtc: String? = null, val status: String? = null,
)
