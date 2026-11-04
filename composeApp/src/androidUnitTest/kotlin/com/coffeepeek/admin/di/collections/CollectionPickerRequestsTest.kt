package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionPickerRequestsTest {
    @Test fun bothHeartStatesOpenTheSamePickerWithoutWritingUntilDone() = runTest {
        val requests = CollectionPickerRequests(CheckInTestSessions(), backgroundScope)
        val target = CollectionTarget(CollectionTargetKind.CoffeeShop, "canonical")
        assertTrue(requests.request(target))
        val first = assertNotNull(requests.state.value)
        assertEquals(target, first.target)
        requests.dismiss(first)
        assertTrue(requests.request(target))
        assertEquals(target, requests.state.value?.target)
    }

    @Test fun guestRequestResumesPickerAfterSignInWithoutAnEagerWrite() = runTest {
        val sessions = CheckInTestSessions(null)
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val target = CollectionTarget(CollectionTargetKind.Roaster, "roaster")
        assertFalse(requests.request(target))
        assertNull(requests.state.value)
        sessions.applySession(Session("token", userId = "owner"))
        runCurrent()
        assertEquals(target, requests.state.value?.target)
    }

    @Test fun replacingTheSameAccountSessionInvalidatesEarlierPickerLease() = runTest {
        val source = CheckInTestSessions()
        var generation = 0L
        val sessions = object : SessionReader by source { override fun accountGeneration() = generation }
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val previous = requests.lease("owner")
        generation++
        source.applySession(Session("replacement", userId = "owner"))
        runCurrent()
        requests.added(previous, "old")
        assertNull(requests.state.value)
    }

    @Test fun successfulAddDeliversCanonicalTargetAndDismissalDoesNotWriteFavorites() = runTest {
        val requests = CollectionPickerRequests(CheckInTestSessions(), backgroundScope)
        requests.added(requests.lease("owner"), "canonical")
        val request = assertNotNull(requests.state.value)
        assertEquals(CollectionTarget(CollectionTargetKind.CoffeeShop, "canonical"), request.target)
        requests.dismiss(request)
        assertNull(requests.state.value)
    }

    @Test fun guestOrDifferentAccountCannotCreateARequest() = runTest {
        val requests = CollectionPickerRequests(CheckInTestSessions(null), backgroundScope)
        assertNull(requests.lease("owner"))
        requests.added(CollectionPickerRequests.Lease("owner", 0), "canonical")
        assertNull(requests.state.value)
    }

    @Test fun logoutClearsTheSheetAndAnOldLeaseCannotReopenAfterSigningBackIn() = runTest {
        val sessions = CheckInTestSessions()
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val lease = requests.lease("owner")
        requests.added(lease, "canonical")
        sessions.applySession(null)
        runCurrent()
        assertNull(requests.state.value)
        sessions.applySession(Session("new", userId = "owner"))
        runCurrent()
        requests.added(lease, "canonical")
        assertNull(requests.state.value)
    }

    @Test fun replacingAccountsRejectsAnEarlierRequestAndTokenRenewalKeepsTheCurrentOne() = runTest {
        val sessions = CheckInTestSessions()
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val previous = requests.lease("owner")
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        requests.added(previous, "old")
        assertNull(requests.state.value)
        requests.added(requests.lease("other"), "current")
        sessions.applySession(Session("renewed", userId = "other"))
        runCurrent()
        assertEquals("current", requests.state.value?.target?.slug)
    }

    @Test fun oldDismissalDoesNotCloseANewerTargetAndUndeliveredRequestsExpire() = runTest {
        val requests = CollectionPickerRequests(CheckInTestSessions(), backgroundScope)
        requests.added(requests.lease("owner"), "one")
        val earlier = assertNotNull(requests.state.value)
        requests.added(requests.lease("owner"), "two")
        requests.dismiss(earlier)
        assertEquals("two", requests.state.value?.target?.slug)
        advanceTimeBy(60_000)
        runCurrent()
        assertNull(requests.state.value)
    }

    @Test fun aPresentedSheetDoesNotExpireWhileTheUserChoosesCollections() = runTest {
        val requests = CollectionPickerRequests(CheckInTestSessions(), backgroundScope)
        requests.added(requests.lease("owner"), "canonical")
        val request = assertNotNull(requests.state.value)
        requests.presented(request)
        advanceTimeBy(90_000)
        runCurrent()
        assertSame(request, requests.state.value)
    }
}
