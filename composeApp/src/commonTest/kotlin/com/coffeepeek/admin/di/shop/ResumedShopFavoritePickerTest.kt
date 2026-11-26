@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.di.collections.CollectionPickerRequests
import com.coffeepeek.core.session.Session
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Replaces the retired automatic writer's regressions with the explicit picker contract. */
class ResumedShopFavoritePickerTest {
    @Test fun guestFavoriteResumesPickerOnceAfterSignIn() = runTest {
        val sessions = CheckInTestSessions(null)
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val intent = ShopFavoritePickerIntent(sessions, requests, backgroundScope)
        runCurrent()
        intent.remember("shop-1", true)
        assertNull(requests.state.value)
        sessions.applySession(Session("token", userId = "owner"))
        runCurrent()
        assertEquals("shop-1", requests.state.value?.target?.slug)
        requests.dismiss(requests.state.value!!)
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertNull(requests.state.value)
    }

    @Test fun accountReplacementCannotCarryTheOldPickerIntoTheNewAccount() = runTest {
        val sessions = CheckInTestSessions(null)
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val intent = ShopFavoritePickerIntent(sessions, requests, backgroundScope)
        runCurrent()
        intent.remember("shop-1", true)
        sessions.applySession(Session("first-token", userId = "first"))
        runCurrent()
        assertEquals("shop-1", requests.state.value?.target?.slug)
        sessions.applySession(Session("second-token", userId = "second"))
        runCurrent()
        assertNull(requests.state.value)
    }

    @Test fun nothingIsOpenedWithoutAnIntent() = runTest {
        val sessions = CheckInTestSessions(null)
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        ShopFavoritePickerIntent(sessions, requests, backgroundScope)
        sessions.applySession(Session("token", userId = "owner"))
        runCurrent()
        assertNull(requests.state.value)
    }
}
