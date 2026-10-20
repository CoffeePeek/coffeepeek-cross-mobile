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
    @Test fun onlyCurrentAccountTokenIsRegisteredAndLogoutRevokesIt() = runTest {
        val sessions = Sessions()
        val platform = Platform()
        val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        runCurrent()
        sessions.applySession(Session("access-a", userId = "alice")); runCurrent()
        platform.mutable.value = platform.mutable.value.copy(userId = "bob", token = "old-token")
        runCurrent()
        assertTrue(repository.registrations.isEmpty())
        platform.mutable.value = platform.mutable.value.copy(userId = "alice", token = "alice-token")
        runCurrent()
        assertEquals(listOf("alice-token"), repository.registrations)
        feature.beforeLogout(); runCurrent()
        assertEquals(1, repository.removals)
        assertNull(platform.state.value.userId)
        feature.foreground(); runCurrent()
        assertEquals(listOf("alice-token"), repository.registrations)
    }

    @Test fun allCategoriesAreEnabledForEachAccountWithoutAskingPermissionOnLogin() = runTest {
        val sessions = Sessions(); val repository = Repository(); val platform = Platform()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access-a", userId = "alice")); runCurrent()
        assertEquals(NotificationPreferences(true, true, true), repository.value)
        val saves = repository.saves
        feature.foreground(); runCurrent()
        assertEquals(saves, repository.saves)
        sessions.applySession(null); runCurrent()
        repository.value = NotificationPreferences(false, false, false)
        sessions.applySession(Session("access-b", userId = "bob")); runCurrent()
        assertEquals(NotificationPreferences(true, true, true), repository.value)
        assertEquals(saves + 1, repository.saves)
        assertEquals(0, platform.permissionRequests)
    }

    @Test fun failedDefaultsAreRetriedOnForegroundWithoutRequestingPermission() = runTest {
        val sessions = Sessions(); val repository = Repository().apply { failSave = true }; val platform = Platform()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        assertFalse(repository.value.inactivity)
        repository.failSave = false
        feature.foreground(); runCurrent()
        assertTrue(repository.value.inactivity)
        assertEquals(0, platform.permissionRequests)
    }

    @Test fun onlySuccessfulCheckInEventRequestsMissingPermissionForSignedInDevice() = runTest {
        val sessions = Sessions(); val platform = Platform()
        platform.mutable.update { it.copy(permissionGranted = false) }
        val feature = NotificationPresenter(Repository(), sessions, platform, backgroundScope)
        runCurrent()
        feature.onCheckInCreated()
        assertEquals(0, platform.permissionRequests)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        feature.foreground(); runCurrent()
        assertEquals(0, platform.permissionRequests)
        feature.onCheckInCreated()
        assertEquals(1, platform.permissionRequests)
        platform.mutable.update { it.copy(permissionGranted = true) }; runCurrent()
        feature.onCheckInCreated()
        assertEquals(1, platform.permissionRequests)
        platform.mutable.update { it.copy(permissionGranted = false, available = false) }; runCurrent()
        feature.onCheckInCreated()
        assertEquals(1, platform.permissionRequests)
        platform.mutable.update { it.copy(available = true) }; runCurrent()
        feature.beforeLogout()
        feature.onCheckInCreated()
        assertEquals(1, platform.permissionRequests)
    }

    @Test fun permissionRevocationRemovesRegistrationAndActivityIsThrottled() = runTest {
        val sessions = Sessions(); val platform = Platform(); val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, platform, backgroundScope)
        sessions.applySession(Session("access", userId = "alice")); runCurrent()
        feature.foreground(); feature.foreground(); runCurrent()
        assertEquals(1, repository.activities)
        platform.mutable.value = platform.mutable.value.copy(permissionGranted = false); runCurrent()
        assertEquals(1, repository.removals)
    }
}

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
    val mutable = MutableStateFlow(PushDeviceState("installation", true, true))
    override val state = mutable.asStateFlow()
    override fun setAccount(userId: String?) { mutable.update { it.copy(userId = userId, token = null) } }
    override fun requestPermission() { permissionRequests++ }
}

private class Repository : NotificationRepository {
    var value = NotificationPreferences(false, false, false); var failSave = false; var saves = 0
    val registrations = mutableListOf<String>(); var removals = 0; var activities = 0
    override suspend fun save(value: NotificationPreferences) { if (failSave) error("offline"); this.value = value; saves++ }
    override suspend fun register(installationId: String, token: String) { registrations += token }
    override suspend fun remove(installationId: String) { removals++ }
    override suspend fun activity() { activities++ }
}
