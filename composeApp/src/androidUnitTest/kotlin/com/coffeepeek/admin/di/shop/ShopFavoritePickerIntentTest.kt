@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.di.collections.CollectionPickerRequests
import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopFavoritePickerIntentTest {
    @Test fun guestHeartOpensPickerOnceAfterSignInWithoutWritingFavorite() = runTest {
        val sessions = CheckInTestSessions(null)
        val requests = CollectionPickerRequests(sessions, backgroundScope)
        val intent = ShopFavoritePickerIntent(sessions, requests, backgroundScope)
        runCurrent()
        intent.remember("shop-1", true)
        assertNull(requests.state.value)
        sessions.applySession(Session("token", userId = "owner"))
        runCurrent()
        assertEquals(CollectionTarget(CollectionTargetKind.CoffeeShop, "shop-1"), requests.state.value?.target)
        val request = requests.state.value!!
        requests.dismiss(request)
        sessions.applySession(Session("renewed", userId = "owner"))
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
