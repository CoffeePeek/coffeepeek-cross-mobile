package com.coffeepeek.admin.ui

import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalSerializationApi::class)
class VisitedPlacesRouteTest {
    @Test
    fun restoredRouteKeepsTheSerializedIdentityOfTheReleasedDestination() {
        val route = Navigator.Screen.VisitedPlaces.serializer().descriptor
        assertEquals("com.coffeepeek.admin.ui.Navigator.Screen.VisitedPlaces", route.serialName)
        assertEquals(0, route.elementsCount)
    }

    @Test
    fun freshNavigationDoesNotUseTheRestorationRoute() {
        with(Navigator) { assertFalse(Navigator.Screen.VisitedPlaces.isHandledByRootNav()) }
    }
}
