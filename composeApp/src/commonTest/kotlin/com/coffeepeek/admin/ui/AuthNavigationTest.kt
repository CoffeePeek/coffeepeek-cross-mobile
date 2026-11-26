package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthNavigationTest {
    @Test fun switchingFormsReplacesOnlyTheCurrentForm() {
        assertTrue(replacesAuthForm(Navigator.Screen.Auth, Navigator.Screen.Register))
        assertTrue(replacesAuthForm(Navigator.Screen.Register, Navigator.Screen.Auth))
        assertFalse(replacesAuthForm(Navigator.Screen.Auth, Navigator.Screen.Auth))
    }

    @Test fun entryRecoveryAndCheckInContinuationKeepTheirOwnBackStack() {
        for (route in listOf(null, Navigator.Screen.Main, Navigator.Screen.ShopDetail("shop"),
            Navigator.Screen.ForgotPassword(), Navigator.Screen.ShopCheckInSignIn("shop"))) {
            assertFalse(replacesAuthForm(route, Navigator.Screen.Auth))
            assertFalse(replacesAuthForm(route, Navigator.Screen.Register))
        }
        assertFalse(replacesAuthForm(Navigator.Screen.Auth, Navigator.Screen.ForgotPassword()))
    }
}
