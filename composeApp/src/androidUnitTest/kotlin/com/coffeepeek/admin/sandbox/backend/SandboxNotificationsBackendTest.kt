package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.notifications.data.createNotificationRepository
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
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

    private fun backend() = SandboxBackend(byteArrayOf(1, 2, 3), LocalDate.parse("2026-10-25"))
}
