package com.coffeepeek.feature.notifications.impl

import com.coffeepeek.feature.notifications.api.NotificationDestination
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.feature.notifications.api.NotificationItem
import com.coffeepeek.feature.notifications.api.NotificationKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosNotificationInboxSessionTest {
    @Test
    fun visibleInboxWaitsForSettingsAndOffersPermissionOnce() = runTest {
        val feature = FakeInbox()
        val session = IosNotificationInboxSession(feature, { _, _, _ -> false }, {}, backgroundScope.coroutineContext)
        session.show()
        val version = session.current().sheetVersion
        assertEquals(1, feature.refreshes)
        session.appeared(version)
        assertEquals(0, feature.permissionOffers)
        feature.state.value = feature.state.value.copy(settingsLoaded = true)
        runCurrent()
        session.appeared(version)
        assertEquals(1, feature.permissionOffers)
    }

    @Test
    fun replacementAccountClearsRowsAndRejectsStaleActions() = runTest {
        val feature = FakeInbox()
        var cancelled = false
        val session = IosNotificationInboxSession(feature, { _, _, _ ->
            try { awaitCancellation() } finally { cancelled = true }
        }, {}, backgroundScope.coroutineContext)
        session.show()
        val version = session.current().sheetVersion
        session.appeared(version)
        session.select(version, "notice")
        runCurrent()
        feature.state.value = feature.state.value.copy(accountId = "bob", generation = 2)
        assertTrue(session.current().items.isEmpty())
        session.seen(version, "notice")
        session.markAllRead(version)
        runCurrent()
        assertEquals(0L, session.current().sheetVersion)
        assertTrue(cancelled)
        assertEquals(0, feature.reads.size)
        assertEquals(0, feature.markAll)
    }

    @Test
    fun seenAndRetryUseCurrentSheetAndFailedNavigationStaysOpen() = runTest {
        val feature = FakeInbox()
        var opened = ""
        val session = IosNotificationInboxSession(feature, { id, account, generation ->
            opened = "$id/$account/$generation"
            false
        }, {}, backgroundScope.coroutineContext)
        session.show()
        val version = session.current().sheetVersion
        session.appeared(version)
        session.seen(version, "notice")
        session.select(version, "notice")
        runCurrent()
        assertEquals("notice/alice/1", opened)
        assertEquals(listOf("notice"), feature.reads)
        assertNull(session.current().openingId)
        assertEquals(version, session.current().sheetVersion)
        session.loadMore(version)
        assertEquals(1, feature.pages)
    }

    @Test
    fun signedOutInboxNeverOffersPermissionAndClosesBeforeSignIn() = runTest {
        val feature = FakeInbox(NotificationInboxState())
        var signIns = 0
        val session = IosNotificationInboxSession(feature, { _, _, _ -> false },
            { signIns++ }, backgroundScope.coroutineContext)
        session.show()
        val version = session.current().sheetVersion
        session.appeared(version)
        assertFalse(session.current().signedIn)
        session.signIn(version)
        assertEquals(1, signIns)
        assertEquals(0L, session.current().sheetVersion)
        assertEquals(0, feature.permissionOffers)
    }

    private class FakeInbox(initial: NotificationInboxState = signedIn()) : NotificationFeature {
        override val state = MutableStateFlow(initial)
        var refreshes = 0
        var pages = 0
        var permissionOffers = 0
        var markAll = 0
        val reads = mutableListOf<String>()
        override fun refresh() { refreshes++ }
        override fun loadMore() { pages++ }
        override fun setPushEnabled(enabled: Boolean) = Unit
        override fun markRead(id: String) { reads += id }
        override fun markAllRead() { markAll++ }
        override suspend fun destination(notificationId: String): NotificationDestination? = null
        override fun foreground() = Unit
        override fun onCheckInCreated() = Unit
        override fun onInboxOpened() { permissionOffers++ }
        override suspend fun beforeLogout() = Unit
    }

    private companion object {
        fun signedIn() = NotificationInboxState(accountId = "alice", generation = 1, unreadCount = 1,
            items = listOf(NotificationItem("notice", NotificationKind.NewShop, "Cafe", "",
                "2026-11-17T08:00:00Z", read = false, canOpen = true)))
    }
}
