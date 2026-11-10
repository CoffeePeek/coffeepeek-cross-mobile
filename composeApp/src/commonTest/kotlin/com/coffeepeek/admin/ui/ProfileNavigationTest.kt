package com.coffeepeek.admin.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileNavigationTest {
    @Test
    fun guestOpensLoginForPublicProfileAndItsCheckIns() {
        val profile = Navigator.Screen.UserProfile("anna")
        val checkIns = Navigator.Screen.UserCheckIns("anna")
        val shop = Navigator.Screen.ShopDetail("shop")

        assertEquals(Navigator.Screen.Auth, profile.profileDestination(isLoggedIn = false))
        assertEquals(Navigator.Screen.Auth, checkIns.profileDestination(isLoggedIn = false))
        assertEquals(shop, shop.profileDestination(isLoggedIn = false))
        assertEquals(profile, profile.profileDestination(isLoggedIn = true))
        assertEquals(checkIns, checkIns.profileDestination(isLoggedIn = true))
    }

    @Test
    fun repeatedProfileUsesItsAddressDespiteDifferentDisplayMetadata() {
        val original = Navigator.Screen.UserProfile("anna", "Anna", "old-avatar")
        val stack = listOf(Navigator.Screen.Main, original, Navigator.Screen.ShopDetail("shop"))
        assertEquals(1, existingProfileIndex(stack, Navigator.Screen.UserProfile("anna", "New name", "new-avatar")))
        assertEquals(-1, existingProfileIndex(stack, Navigator.Screen.UserProfile("boris", "Anna", "old-avatar")))
        assertEquals(-1, existingProfileIndex(stack, Navigator.Screen.ShopDetail("shop")))
    }

    @Test
    fun latestMatchingProfileIsRestoredWithoutMatchingAnUnrelatedRoute() {
        val profile = Navigator.Screen.UserProfile("anna")
        val stack = listOf(null, profile, Navigator.Screen.UserProfile("boris"), profile.copy(nickname = "Renamed"))
        assertEquals(3, existingProfileIndex(stack, profile))
        assertEquals(-1, existingProfileIndex(emptyList(), profile))
        assertEquals(-1, existingProfileIndex(stack, Navigator.Screen.UserCheckIns("anna")))
        assertEquals(-1, existingProfileIndex(listOf(Navigator.Screen.UserProfile("")), Navigator.Screen.UserProfile("")))
    }
}
