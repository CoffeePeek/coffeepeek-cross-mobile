@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.feature.favorites.ui.TrackedFavoriteInteractions
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

class FavoriteAnalyticsTest {
    @Test fun analyticsSessionObserverFailureDoesNotEscapeIntoTheApplication() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity(null); Analytics.install(service)
        val sessions = object : SessionRepository by AnalyticsSessions() {
            override fun observeSession() = kotlinx.coroutines.flow.flow<Session?> { error("storage unavailable") }
        }
        TrackedFavoriteInteractions(sessions, backgroundScope)
        runCurrent()
        assertFalse(service.state.value.authReady)
        assertFalse(service.state.value.ready)
    }

    @Test fun guestIntentResumesOnceAfterLoginWithoutCollectingGuestEvents() = runTest {
        val sessions = AnalyticsSessions()
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity(null); Analytics.install(service)
        val interactions = TrackedFavoriteInteractions(sessions, backgroundScope)
        val result = CompletableDeferred<Result<Unit>>()
        var mutations = 0
        val action: suspend () -> Result<Unit> = { mutations++; result.await() }
        interactions.change("never-send-id", false, true, action)
        interactions.change("never-send-id", false, true, action)
        runCurrent()
        assertEquals(0, mutations)
        assertTrue(sdk.calls.isEmpty())
        assertTrue(sdk.events.isEmpty())
        sessions.value.value = Session("token-not-for-analytics", userId = "backend-id")
        runCurrent()
        assertEquals(1, mutations)
        assertTrue(sdk.events.none { it.first == "favorite_changed" })
        result.complete(Result.success(Unit)); runCurrent()
        val changed = sdk.events.single { it.first == "favorite_changed" }.second
        assertEquals(true, changed["resumed_after_auth"])
        assertEquals("signed_in", changed["auth_state"])
        assertEquals("server", changed["favorite_storage"])
        assertTrue(sdk.events.none { "never-send-id" in it.second.toString() || "token-not-for-analytics" in it.second.toString() })
    }

    @Test fun oldAccountResultAndCancelledFailureAreExcluded() = runTest {
        val sessions = AnalyticsSessions().apply { value.value = Session("A-token", userId = "A") }
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A"); Analytics.install(service)
        val interactions = TrackedFavoriteInteractions(sessions, backgroundScope)
        val result = CompletableDeferred<Result<Unit>>()
        interactions.change("key", true, true, { result.await() })
        runCurrent()
        sessions.value.value = Session("B-token", userId = "B")
        result.complete(Result.success(Unit)); runCurrent()
        assertTrue(sdk.events.none { it.first == "favorite_changed" })
        interactions.change("key", true, true, { Result.failure(kotlinx.coroutines.CancellationException()) })
        runCurrent()
        assertTrue(sdk.events.none { it.first == "favorite_failed" })
    }
}

private class AnalyticsSessions : SessionRepository {
    val value = MutableStateFlow<Session?>(null)
    override fun peekSession() = value.value
    override fun applySession(session: Session?) { value.value = session }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = value.value
    override suspend fun persistSession(session: Session?) = Unit
    override suspend fun saveSession(session: Session?) { value.value = session }
    override suspend fun warmCache() = Unit
    override fun observeSession() = value
    override suspend fun isLoggedIn() = value.value != null
}
