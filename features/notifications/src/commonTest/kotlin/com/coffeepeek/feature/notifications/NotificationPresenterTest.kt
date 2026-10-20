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

    @Test fun accountSwitchClearsPreferencesAndFailedSaveKeepsLastSavedChoice() = runTest {
        val sessions = Sessions(); val repository = Repository()
        val feature = NotificationPresenter(repository, sessions, Platform(), backgroundScope)
        sessions.applySession(Session("access-a", userId = "alice")); runCurrent()
        feature.save(NotificationPreferences(inactivity = true)); runCurrent()
        assertEquals(true, feature.state.value.preferences?.inactivity)
        repository.failSave = true
        feature.save(NotificationPreferences(inactivity = false)); runCurrent()
        assertEquals(true, feature.state.value.preferences?.inactivity)
        assertNotNull(feature.state.value.error)
        sessions.applySession(null); runCurrent()
        assertFalse(feature.state.value.signedIn)
        assertNull(feature.state.value.preferences)
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
    val mutable = MutableStateFlow(PushDeviceState("installation", true, true))
    override val state = mutable.asStateFlow()
    override fun setAccount(userId: String?) { mutable.update { it.copy(userId = userId, token = null) } }
    override fun requestPermission() {}
}

private class Repository : NotificationRepository {
    var value = NotificationPreferences(); var failSave = false
    val registrations = mutableListOf<String>(); var removals = 0; var activities = 0
    override suspend fun preferences() = value
    override suspend fun save(value: NotificationPreferences) { if (failSave) error("offline"); this.value = value }
    override suspend fun register(installationId: String, token: String) { registrations += token }
    override suspend fun remove(installationId: String) { removals++ }
    override suspend fun activity() { activities++ }
}
