package com.coffeepeek.feature.notifications.impl

import com.coffeepeek.feature.notifications.api.NotificationDestination
import com.coffeepeek.feature.notifications.domain.Notification
import com.coffeepeek.feature.notifications.domain.NotificationTarget
import kotlin.test.*

class NotificationDestinationsTest {
    @Test fun currentPublishedSlugsAndSubmissionIdentitiesRemainDistinct() {
        assertEquals(NotificationDestination.Shop("current-cafe"), NotificationTarget("Shop", "current-cafe").toDestination())
        assertEquals(NotificationDestination.ShopChange("change-id"), NotificationTarget("ShopChange", "change-id").toDestination())
        assertEquals(NotificationDestination.ShopSubmission("submission-id"), NotificationTarget("ShopSubmission", "submission-id").toDestination())
    }

    @Test fun unsupportedAndUnsafeTargetsNeverBecomeRoutes() {
        for (value in listOf("", "../cafe", "https://example.com", "a?user=other", "..", "a\nb")) {
            assertNull(NotificationTarget("Shop", value).toDestination())
        }
        assertNull(NotificationTarget("FutureKind", "cafe").toDestination())
    }

    @Test fun readStateDoesNotRemoveTheActionAndMismatchedMetadataHasNoAction() {
        val item = Notification("notice", "NewShop", "Title", "Body", "2026-10-25T10:00:00Z", null, "Published", "Shop", "id")
        assertTrue(item.canOpen())
        assertTrue(item.copy(readAtUtc = "2026-10-25T11:00:00Z").canOpen())
        assertFalse(item.copy(entityType = "CheckIn").canOpen())
        assertFalse(item.copy(entityId = null).canOpen())
        assertFalse(item.copy(type = "FutureKind").canOpen())
    }
}
