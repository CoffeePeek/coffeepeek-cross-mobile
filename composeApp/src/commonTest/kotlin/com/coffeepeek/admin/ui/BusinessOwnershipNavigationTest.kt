package com.coffeepeek.admin.ui

import kotlinx.serialization.json.Json
import kotlin.test.*

class BusinessOwnershipNavigationTest {
    @Test fun bothPlatformRootsHandleTheOwnershipDestination() {
        with(Navigator) { assertTrue(Navigator.Screen.MyBusiness().isHandledByRootNav()) }
    }
    @Test fun aClaimEntryRetainsOnlyPublicTypeAndSlugInSavedNavigation() {
        val route = Navigator.Screen.MyBusiness("Roaster", "example-roaster")
        assertEquals(route, Json.decodeFromString<Navigator.Screen.MyBusiness>(Json.encodeToString(route)))
    }
    @Test fun businessModeHasExactlyTwoTabsOnBothPlatforms() {
        assertEquals(listOf(Navigator.Screen.BusinessTab, Navigator.Screen.SettingsTab), mainTabScreens(true, true))
        assertEquals(5, nativeTabIndex(Navigator.Screen.BusinessTab))
        assertEquals(4, nativeTabIndex(Navigator.Screen.SettingsTab))
        assertFalse(Navigator.Screen.BusinessTab in mainTabScreens(false, true))
    }
    @Test fun ownerEditorRouteRetainsBusinessIdentity() {
        val route = Navigator.Screen.MyBusiness(kind = "CoffeeShop", businessId = "business-id")
        assertEquals(route, Json.decodeFromString<Navigator.Screen.MyBusiness>(Json.encodeToString(route)))
    }
}
