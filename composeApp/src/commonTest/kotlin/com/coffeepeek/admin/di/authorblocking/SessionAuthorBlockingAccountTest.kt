package com.coffeepeek.admin.di.authorblocking

import com.coffeepeek.admin.di.notifications.SessionNotificationAccount
import com.coffeepeek.admin.session.ReadOnlySessions
import com.coffeepeek.core.session.Session
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionAuthorBlockingAccountTest {
    @Test fun readerKeepsOpaqueFallbackOnlyForAuthorBlocking() = runTest {
        val sessions = ReadOnlySessions(Session("opaque-token"))
        val blocking = SessionAuthorBlockingAccount(sessions)
        val notifications = SessionNotificationAccount(sessions)
        assertEquals("opaque-token", blocking.current())
        assertEquals("opaque-token", blocking.changes.first())
        assertNull(notifications.userId)
        assertNull(notifications.observeUserId().first())
        sessions.state.value = Session("new-token", userId = "A")
        assertEquals("A", blocking.current())
        assertEquals("A", notifications.userId)
    }

    @Test fun inactiveAndSignedOutReadersExposeNoAccount() = runTest {
        val sessions = ReadOnlySessions(Session("token", userId = "A"))
        val blocking = SessionAuthorBlockingAccount(sessions)
        val notifications = SessionNotificationAccount(sessions)
        sessions.active = false
        assertNull(blocking.current())
        assertNull(blocking.changes.first())
        assertNull(notifications.userId)
        assertNull(notifications.observeUserId().first())
        sessions.state.value = null
        assertNull(blocking.current())
        assertNull(notifications.userId)
    }
}
