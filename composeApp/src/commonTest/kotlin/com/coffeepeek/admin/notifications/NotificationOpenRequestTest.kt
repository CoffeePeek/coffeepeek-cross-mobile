package com.coffeepeek.admin.notifications

import kotlin.test.*

class NotificationOpenRequestTest {
    @AfterTest fun clear() { NotificationOpens.pending.value?.let(NotificationOpens::consume) }

    @Test fun aTapWaitsForTheHostAndConsumingAnOlderTapCannotEraseANewerOne() {
        NotificationOpens.submit(mapOf("notificationId" to "first", "userId" to "alice"))
        val first = NotificationOpens.pending.value!!
        assertEquals("first", first.id)
        NotificationOpens.submit(mapOf("notificationId" to "second", "userId" to "alice"))
        NotificationOpens.consume(first)
        assertEquals("second", NotificationOpens.pending.value?.id)
    }

    @Test fun missingOwnershipAndMalformedIdsDoNotCreateAnOpenRequest() {
        NotificationOpens.submit(mapOf("notificationId" to "notice"))
        assertNull(NotificationOpens.pending.value)
        for (id in listOf("", "../other", "a?b", "a\nb")) {
            NotificationOpens.submit(mapOf("notificationId" to id, "userId" to "alice"))
            assertNull(NotificationOpens.pending.value)
        }
    }
}
