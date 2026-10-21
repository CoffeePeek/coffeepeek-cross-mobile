package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidCheckInHistoryHostViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun editRouteIsConsumedOnceAndIsNotReplayedForAReplacementAccount() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        assertNull(host.consumeInitialHomeId(null))
        assertNull(host.consumeInitialHomeId(""))
        assertEquals("entry", host.consumeInitialHomeId("entry"))
        assertNull(host.consumeInitialHomeId("entry"))
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        assertNull(host.consumeInitialHomeId("entry"))
        assertEquals("another", host.consumeInitialHomeId("another"))
    }

    @Test fun hiddenRouteAccountReplacementClearsChildrenButTokenRenewalDoesNot() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val initial = host.session.value
        val child = Probe().also { initial.viewModelStore.put("history", it) }
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertSame(initial, host.session.value)
        assertFalse(child.cleared)
        sessions.applySession(null)
        runCurrent()
        assertTrue(child.cleared)
        assertFalse(initial.lease.isCurrent())
        assertFalse(host.session.value.lease.isLoggedIn)
        assertEquals(1, host.session.value.generation)
        val guest = host.session.value
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        assertFalse(guest.lease.isCurrent())
        assertTrue(host.session.value.lease.isLoggedIn)
        assertEquals(2, host.session.value.generation)
    }

    @Test fun ownerDisposalRevokesCallbacksAndClearsFeatureStore() = runTest(dispatcher) {
        val host = AndroidCheckInHistoryHostViewModel(CheckInTestSessions()).also { store.put("host", it) }
        val session = host.session.value
        val child = Probe().also { session.viewModelStore.put("history", it) }
        session.openPhoto(listOf("https://fixture.invalid/private.jpg"), 0)
        store.clear()
        assertTrue(child.cleared)
        assertFalse(session.lease.isCurrent())
        assertNull(session.photoPreview.value)
    }

    @Test fun observationFailureHidesDataUntilExplicitRetryWithFreshStore() = runTest(dispatcher) {
        var fail = true
        val source = CheckInTestSessions()
        val sessions = object : SessionRepository by source {
            override fun observeSession() = if (fail) flow<Session?> { error("Store unavailable") } else source.observeSession()
        }
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val failed = host.session.value
        assertTrue(failed.observationFailed)
        fail = false
        host.retry()
        runCurrent()
        assertFalse(host.session.value.observationFailed)
        assertFalse(failed.lease.isCurrent())
        assertTrue(host.session.value.lease.isCurrent())
        assertEquals(failed.generation + 1, host.session.value.generation)
    }

    @Test fun accountReplacementClearsAnAlreadyOpenPhotoAndStaleCallbacksCannotReopenIt() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val previous = host.session.value
        previous.openPhoto(listOf("https://fixture.invalid/first.jpg", "https://fixture.invalid/second.jpg"), 1)
        assertEquals(1, previous.photoPreview.value?.initialIndex)
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        assertNull(previous.photoPreview.value)
        assertNull(host.session.value.photoPreview.value)
        previous.openPhoto(listOf("https://fixture.invalid/private.jpg"), 0)
        assertNull(previous.photoPreview.value)
        assertNull(host.session.value.photoPreview.value)
    }

    @Test fun photoViewerCopiesTheAlbumAndRejectsGuestOrInvalidIndices() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val current = host.session.value
        val photos = mutableListOf("https://fixture.invalid/photo.jpg")
        current.openPhoto(photos, 0)
        photos.clear()
        assertEquals(listOf("https://fixture.invalid/photo.jpg"), current.photoPreview.value?.urls)
        current.dismissPhoto()
        current.openPhoto(emptyList(), 0)
        current.openPhoto(listOf(" "), 0)
        current.openPhoto(listOf("url"), -1)
        current.openPhoto(listOf("url"), 1)
        assertNull(current.photoPreview.value)
        sessions.applySession(null)
        runCurrent()
        host.session.value.openPhoto(listOf("url"), 0)
        assertNull(host.session.value.photoPreview.value)
    }

    @Test fun explicitOriginalsStayAlignedAndCannotSurviveAccountRevocation() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val current = host.session.value
        val details = mutableListOf("https://fixture.invalid/detail", "")
        val originals = mutableListOf("https://fixture.invalid/one", "https://fixture.invalid/two")
        current.openPhoto(details, originals, 1)
        details.clear(); originals.clear()
        assertEquals(listOf("https://fixture.invalid/detail", ""), current.photoPreview.value?.urls)
        assertEquals(listOf("https://fixture.invalid/one", "https://fixture.invalid/two"), current.photoPreview.value?.originalUrls)
        current.dismissPhoto()
        current.openPhoto(listOf("detail"), emptyList(), 0)
        assertNull(current.photoPreview.value)
        sessions.applySession(null); runCurrent()
        current.openPhoto(listOf(""), listOf("private-original"), 0)
        assertNull(current.photoPreview.value)
    }

    @Test fun tokenRenewalPreservesAnOpenPhotoWithoutPreservingItAcrossLogout() = runTest(dispatcher) {
        val sessions = CheckInTestSessions()
        val host = AndroidCheckInHistoryHostViewModel(sessions).also { store.put("host", it) }
        val current = host.session.value
        current.openPhoto(listOf("https://fixture.invalid/photo.jpg"), 0)
        val photo = current.photoPreview.value
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertSame(current, host.session.value)
        assertSame(photo, current.photoPreview.value)
        sessions.applySession(null)
        runCurrent()
        assertNull(current.photoPreview.value)
        sessions.applySession(Session("next", userId = "other"))
        runCurrent()
        assertNull(host.session.value.photoPreview.value)
    }

    private class Probe : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
