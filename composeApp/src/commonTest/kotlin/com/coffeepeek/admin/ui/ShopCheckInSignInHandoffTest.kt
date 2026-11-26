package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopCheckInSignInHandoffTest {
    @Test
    fun authenticationCanResumeTheScannedShopOnlyOnceAfterTheRouteDisposes() {
        val handoff = ShopCheckInSignInHandoff()
        var activeSession = false
        val attempt = handoff.attempt("marks") { activeSession }
        attempt.begin()

        activeSession = true
        attempt.dispose()
        assertEquals(Navigator.Screen.ShopCheckInEditor("marks"), handoff.take())
        assertNull(handoff.take())
    }

    @Test
    fun explicitlyCancellingTheCurrentSignInDiscardsItsCheckIn() {
        val handoff = ShopCheckInSignInHandoff()
        val attempt = handoff.attempt("marks") { true }
        attempt.begin()
        attempt.cancel()
        assertNull(handoff.take())
    }

    @Test
    fun leavingSignInWithoutAnActiveSessionDiscardsItsCheckIn() {
        val handoff = ShopCheckInSignInHandoff()
        val attempt = handoff.attempt("marks") { false }
        attempt.begin()
        attempt.dispose()
        assertNull(handoff.take())
    }

    @Test
    fun disposalOfAnOlderScanCannotCancelANewerScanOfTheSameShop() {
        val handoff = ShopCheckInSignInHandoff()
        val older = handoff.attempt("marks") { false }
        older.begin()
        handoff.attempt("marks") { true }.begin()
        older.dispose()
        assertEquals(Navigator.Screen.ShopCheckInEditor("marks"), handoff.take())
    }
}
