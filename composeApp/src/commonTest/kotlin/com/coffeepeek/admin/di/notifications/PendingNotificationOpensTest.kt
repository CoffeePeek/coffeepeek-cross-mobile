package com.coffeepeek.admin.di.notifications

import com.coffeepeek.admin.notifications.NotificationOpenRequest
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PendingNotificationOpensTest {
    private val requests = MutableStateFlow<NotificationOpenRequest?>(NotificationOpenRequest("one", "owner"))
    private val inbox = MutableStateFlow(NotificationInboxState())
    private val loggedIn = MutableStateFlow(true)
    private val opened = mutableListOf<String>()
    private fun collector(open: suspend (NotificationOpenRequest, Long) -> Unit = { request, _ -> opened.add(request.id) }) =
        PendingNotificationOpens(inbox, { "owner" }, open, requests, { requests.compareAndSet(it, null) })

    @Test fun tapWaitsUntilTheMatchingAccountIsReady() = runTest {
        backgroundScope.launch { collector().observe(loggedIn) }
        runCurrent()
        assertNotNull(requests.value)
        assertTrue(opened.isEmpty())
        inbox.value = NotificationInboxState(accountId = "owner", generation = 4)
        runCurrent()
        assertEquals(listOf("one"), opened)
        assertNull(requests.value)
    }

    @Test fun cancellingAnOldTapCannotConsumeANewerTap() = runTest {
        inbox.value = NotificationInboxState(accountId = "owner", generation = 4)
        val cancelled = mutableListOf<String>()
        val source = collector { request, _ ->
            try { awaitCancellation() } finally { cancelled.add(request.id) }
        }
        val observation = backgroundScope.launch { source.observe(loggedIn) }
        runCurrent()
        requests.value = NotificationOpenRequest("two", "owner")
        runCurrent()
        assertEquals(listOf("one"), cancelled)
        assertEquals("two", requests.value?.id)
        observation.cancel()
        runCurrent()
        assertEquals(listOf("one", "two"), cancelled)
    }

    @Test fun replacingTheAccountCancelsTheOutstandingResolution() = runTest {
        inbox.value = NotificationInboxState(accountId = "owner", generation = 4)
        var cancelled = false
        val source = collector { _, _ -> try { awaitCancellation() } finally { cancelled = true } }
        backgroundScope.launch { source.observe(loggedIn) }
        runCurrent()
        inbox.value = NotificationInboxState(accountId = "replacement", generation = 5)
        runCurrent()
        assertTrue(cancelled)
        assertTrue(opened.isEmpty())
        assertNull(requests.value)
    }

    @Test fun guestConsumesTheTapWithoutResolvingPrivateContent() = runTest {
        loggedIn.value = false
        backgroundScope.launch { collector().observe(loggedIn) }
        runCurrent()
        assertNull(requests.value)
        assertTrue(opened.isEmpty())
    }
}
