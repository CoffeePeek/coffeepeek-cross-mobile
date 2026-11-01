package com.coffeepeek.admin.ui

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationSettingsNavigationTest {
    @Test fun bothPlatformRootsHandleNotificationPreferencesOutsideTheInbox() {
        with(Navigator) { assertTrue(Navigator.Screen.NotificationSettings.isHandledByRootNav()) }
    }

    @Test fun savedNotificationSettingsRouteContainsNoAccountOrPermissionState() {
        val route = Navigator.Screen.NotificationSettings
        assertEquals(route, Json.decodeFromString<Navigator.Screen.NotificationSettings>(Json.encodeToString(route)))
    }
}
