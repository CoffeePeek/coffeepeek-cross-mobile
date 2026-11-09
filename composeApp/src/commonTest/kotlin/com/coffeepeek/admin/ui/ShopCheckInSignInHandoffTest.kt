package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopCheckInSignInHandoffTest {
    @Test
    fun authenticationCanResumeTheScannedShopOnlyOnce() {
        val handoff = ShopCheckInSignInHandoff()
        handoff.begin(ShopCheckInSignInHandoff.Request("marks"))

        assertEquals(Navigator.Screen.ShopDetail("marks", openCheckIn = true), handoff.take())
        assertNull(handoff.take())
    }

    @Test
    fun cancellingTheCurrentSignInDiscardsItsCheckIn() {
        val handoff = ShopCheckInSignInHandoff()
        val request = ShopCheckInSignInHandoff.Request("marks")
        handoff.begin(request)
        handoff.cancel(request)

        assertNull(handoff.take())
    }

    @Test
    fun disposalOfAnOlderScanCannotCancelANewerScanOfTheSameShop() {
        val handoff = ShopCheckInSignInHandoff()
        val older = ShopCheckInSignInHandoff.Request("marks")
        handoff.begin(older)
        handoff.begin(ShopCheckInSignInHandoff.Request("marks"))
        handoff.cancel(older)

        assertEquals(Navigator.Screen.ShopDetail("marks", openCheckIn = true), handoff.take())
    }
}
