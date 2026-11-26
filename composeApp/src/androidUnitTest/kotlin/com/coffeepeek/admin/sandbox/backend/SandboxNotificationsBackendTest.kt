package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.notifications.data.createNotificationRepository
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class SandboxNotificationsBackendTest {
    @Test fun destinationsPreservePublishedShopAndCorrectionIdentitiesWithoutReadingTheRow() = runTest {
        val backend = backend()
        try {
            val repository = createNotificationRepository(backend.gateway)
            assertEquals(6, repository.inbox(null).unreadCount)
            val shop = repository.destination(SandboxNotificationsBackend.NEW_SHOP)
            assertEquals("Shop", shop?.kind)
            assertEquals("sandbox-lab", shop?.value)
            val correction = repository.destination(SandboxNotificationsBackend.CORRECTION)
            assertEquals("ShopChange", correction?.kind)
            assertEquals("change-approved", correction?.value)
            assertEquals(6, repository.inbox(null).unreadCount)
            repository.markRead(SandboxNotificationsBackend.NEW_SHOP)
            assertEquals(5, repository.inbox(null).unreadCount)
        } finally {
            backend.close()
        }
    }

    @Test fun readRowsRemainResolvableAndGeneralMessagesHaveNoDestination() = runTest {
        val backend = backend()
        try {
            val repository = createNotificationRepository(backend.gateway)
            assertEquals("sandbox-cafe", repository.destination(SandboxNotificationsBackend.READ_SHOP)?.value)
            assertNotNull(repository.inbox(null).items.single { it.id == SandboxNotificationsBackend.READ_SHOP }.readAtUtc)
            assertNull(repository.destination(SandboxNotificationsBackend.GENERAL))
            assertFailsWith<ClientRequestException> { repository.destination(SandboxNotificationsBackend.MISSING) }
            assertFailsWith<ClientRequestException> { repository.destination("unknown") }
        } finally {
            backend.close()
        }
    }

    @Test fun cityContractRequiresAnExplicitValueAndSupportsClearing() {
        val notifications = SandboxNotificationsBackend(LocalDate.parse("2026-10-25"))
        val path = "/api/v1/me/notifications/city"
        val put = io.ktor.http.HttpMethod.Put
        val json = kotlinx.serialization.json.Json
        assertEquals(io.ktor.http.HttpStatusCode.BadRequest, notifications.handle(path, put, json.parseToJsonElement("{}").jsonObject)?.first)
        val selected = json.parseToJsonElement("""{"citySlug":"minsk"}""").jsonObject
        assertEquals(io.ktor.http.HttpStatusCode.NoContent, notifications.handle(path, put, selected)?.first)
        assertEquals("minsk", notifications.selectedCitySlug)
        val cleared = json.parseToJsonElement("""{"citySlug":null}""").jsonObject
        assertEquals(io.ktor.http.HttpStatusCode.NoContent, notifications.handle(path, put, cleared)?.first)
        assertNull(notifications.selectedCitySlug)
    }

    private fun backend() = SandboxBackend(byteArrayOf(1, 2, 3), LocalDate.parse("2026-10-25"))
}
