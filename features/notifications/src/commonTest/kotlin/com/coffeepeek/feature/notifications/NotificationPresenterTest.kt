package com.coffeepeek.feature.notifications

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.notifications.api.*
import com.coffeepeek.feature.notifications.domain.*
import com.coffeepeek.feature.notifications.ui.NotificationPresenter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPresenterTest {
    @Test fun onlyCurrentAccountTokenIsRegisteredAndLogoutClearsInbox() = runTest {
        val sessions = Sessions(); val platform = Platform(); val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access-a", userId = "alice")); runCurrent()
        platform.mutable.update { it.copy(userId = "bob", token = "old-token") }; runCurrent()
        assertTrue(repository.registrations.isEmpty())
        platform.mutable.update { it.copy(userId = "alice", token = "alice-token") }; runCurrent()
        assertEquals(listOf("alice-token"), repository.registrations)
        assertEquals("first", feature.state.value.items.single().id)
        feature.beforeLogout(); runCurrent()
        assertNull(feature.state.value.accountId)
        assertTrue(feature.state.value.items.isEmpty())
        assertNull(platform.state.value.userId)
        feature.foreground(); runCurrent()
        assertEquals(listOf("alice-token"), repository.registrations)
        assertEquals(1, repository.removals)
    }

    @Test fun loginAndForegroundPreserveDisabledPushWithoutRequestingPermission() = runTest {
        val sessions = Sessions(); val repository = Repository().apply { enabled = false }; val platform = Platform()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        feature.foreground(); runCurrent()
        assertFalse(feature.state.value.pushEnabled)
        assertTrue(feature.state.value.settingsLoaded)
        assertTrue(repository.settingsWrites.isEmpty())
        assertEquals(0, platform.permissionRequests)
        feature.onCheckInCreated()
        assertEquals(0, platform.permissionRequests)
        assertTrue(feature.state.value.items.isNotEmpty())
    }

    @Test fun explicitToggleRequestsPermissionAndDenialDoesNotPretendPermissionWasGranted() = runTest {
        val sessions = Sessions(); val platform = Platform(); val repository = Repository()
        platform.mutable.update { it.copy(permissionGranted = false) }
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        feature.setPushEnabled(true); feature.setPushEnabled(true); runCurrent()
        assertEquals(listOf(true), repository.settingsWrites)
        assertEquals(1, platform.explicitPermissionRequests)
        assertFalse(feature.state.value.permissionGranted)
        assertTrue(repository.registrations.isEmpty())
        platform.mutable.update { it.copy(permissionGranted = true, token = "token") }; runCurrent()
        assertTrue(feature.state.value.permissionGranted)
        assertEquals(listOf("token"), repository.registrations)
        feature.setPushEnabled(false); runCurrent()
        assertFalse(feature.state.value.pushEnabled)
        assertFalse(repository.enabled)
    }

    @Test fun settingsFailureCanBeRetriedAndFailedSaveKeepsPreviousValue() = runTest {
        val sessions = Sessions(); val repository = Repository().apply { failSettings = true }; val platform = Platform()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        assertFalse(feature.state.value.settingsLoaded)
        assertNotNull(feature.state.value.settingsError)
        repository.failSettings = false
        feature.refresh(); runCurrent()
        assertTrue(feature.state.value.settingsLoaded)
        repository.failSave = true
        feature.setPushEnabled(false); runCurrent()
        assertTrue(feature.state.value.pushEnabled)
        assertNotNull(feature.state.value.settingsError)
        assertFalse(feature.state.value.savingSettings)
    }

    @Test fun paginationDeduplicatesAndUnknownTypesRemainVisible() = runTest {
        val sessions = Sessions(); val repository = Repository()
        repository.page = { before ->
            if (before == null) NotificationPage(listOf(entry("first")), "first", 2)
            else NotificationPage(listOf(entry("first"), entry("second", "FutureType")), null, 2)
        }
        val feature = NotificationPresenter(repository, sessions, Platform(), backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        feature.loadMore(); runCurrent()
        assertEquals(listOf("first", "second"), feature.state.value.items.map { it.id })
        assertEquals(NotificationKind.General, feature.state.value.items.last().kind)
        assertNull(feature.state.value.nextCursor)
    }

    @Test fun accountSwitchDiscardsLateInboxResponseIncludingReturnToSameAccount() = runTest {
        val sessions = Sessions(); val repository = Repository(); val late = CompletableDeferred<NotificationPage>()
        repository.page = { withContext(NonCancellable) { late.await() } }
        val feature = NotificationPresenter(repository, sessions, Platform(), backgroundScope)
        sessions.applySession(Session("first-access", userId = "alice")); runCurrent()
        sessions.applySession(Session("bob-access", userId = "bob")); runCurrent()
        sessions.applySession(Session("new-access", userId = "alice")); runCurrent()
        val generation = feature.state.value.generation
        repository.page = { NotificationPage(listOf(entry("current")), null, 1) }
        late.complete(NotificationPage(listOf(entry("old")), null, 1)); runCurrent()
        feature.refresh(); runCurrent()
        assertEquals(generation, feature.state.value.generation)
        assertEquals(listOf("current"), feature.state.value.items.map { it.id })
    }

    @Test fun markAllUsesDisplayedSnapshotAndRefreshesUnreadArrivals() = runTest {
        val sessions = Sessions(); val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, Platform(), backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        repository.page = { NotificationPage(listOf(entry("new"), entry("first").copy(readAtUtc = "2026-10-21T09:00:00Z")), null, 1) }
        feature.markAllRead(); runCurrent()
        assertEquals(listOf("first"), repository.readThrough)
        assertEquals(1, feature.state.value.unreadCount)
        assertFalse(feature.state.value.items.first().read)
        assertTrue(feature.state.value.items.last().read)
    }

    @Test fun permissionRevocationRemovesDeviceAndActivityIsThrottled() = runTest {
        val sessions = Sessions(); val platform = Platform(); val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        feature.foreground(); feature.foreground(); runCurrent()
        assertEquals(1, repository.activities)
        feature.onCheckInCreated()
        assertEquals(0, platform.permissionRequests)
        platform.mutable.update { it.copy(permissionGranted = false) }; runCurrent()
        assertTrue(repository.removals > 0)
        feature.onCheckInCreated()
        assertEquals(1, platform.permissionRequests)
    }

    @Test fun foregroundPushUpdatesInboxWithoutClosingThePanel() = runTest {
        val sessions = Sessions(); val platform = Platform(); val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        repository.page = { NotificationPage(listOf(entry("arrived")), null, 1) }
        platform.mutable.update { it.copy(notificationRevision = it.notificationRevision + 1) }; runCurrent()
        assertEquals("arrived", feature.state.value.items.single().id)
    }

    @Test fun pushArrivingDuringAnInboxLoadQueuesAnotherRefresh() = runTest {
        val sessions = Sessions(); val platform = Platform(); val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        val loading = CompletableDeferred<NotificationPage>()
        repository.page = { loading.await() }
        feature.refresh(); runCurrent()
        platform.mutable.update { it.copy(notificationRevision = 1) }; runCurrent()
        repository.page = { NotificationPage(listOf(entry("newest")), null, 1) }
        loading.complete(NotificationPage(listOf(entry("older")), null, 1)); runCurrent()
        assertEquals("newest", feature.state.value.items.single().id)
    }
}

private fun entry(id: String, type: String = "ShopApproved") =
    Notification(id, type, "Кофейня добавлена", "Спасибо за ваш вклад", "2026-10-21T08:00:00Z", null, "Approved")

private class Sessions : SessionRepository {
    val session = MutableStateFlow<Session?>(null)
    override fun peekSession() = session.value
    override fun applySession(session: Session?) { this.session.value = session }
    override fun isActiveSession(session: Session?) = session != null && this.session.value == session
    override suspend fun getSession() = session.value
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() {}
    override fun observeSession() = session
    override suspend fun isLoggedIn() = session.value != null
}

private class Platform : PushDevicePlatform {
    var permissionRequests = 0
    var explicitPermissionRequests = 0
    val mutable = MutableStateFlow(PushDeviceState("installation", true, true))
    override val state = mutable.asStateFlow()
    override fun setAccount(userId: String?) { mutable.update { it.copy(userId = userId, token = null) } }
    override fun requestPermission() { permissionRequests++ }
    override fun requestPermissionFromSettings() { explicitPermissionRequests++ }
}

private class Repository : NotificationRepository {
    var enabled = true
    var failSettings = false
    var failSave = false
    val settingsWrites = mutableListOf<Boolean>()
    val registrations = mutableListOf<String>()
    val readThrough = mutableListOf<String>()
    var removals = 0
    var activities = 0
    var page: suspend (String?) -> NotificationPage = { NotificationPage(listOf(entry("first")), null, 1) }
    override suspend fun inbox(before: String?) = page(before)
    override suspend fun pushEnabled(): Boolean { if (failSettings) error("offline"); return enabled }
    override suspend fun setPushEnabled(enabled: Boolean) { if (failSave) error("offline"); settingsWrites += enabled; this.enabled = enabled }
    override suspend fun markRead(id: String) {}
    override suspend fun markAllRead(throughId: String) { readThrough += throughId }
    override suspend fun register(installationId: String, token: String) { registrations += token }
    override suspend fun remove(installationId: String) { removals++ }
    override suspend fun activity() { activities++ }
}
