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
}
