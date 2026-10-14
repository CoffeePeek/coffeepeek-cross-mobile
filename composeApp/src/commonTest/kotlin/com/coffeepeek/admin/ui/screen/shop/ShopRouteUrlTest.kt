package com.coffeepeek.admin.ui.screen.shop

import kotlin.test.Test
import kotlin.test.assertEquals

class ShopRouteUrlTest {

    @Test
    fun buildsYandexMapsRouteFromCurrentLocation() {
        assertEquals(
            "https://yandex.ru/maps/?mode=routes&rtext=~53.902284,27.561831&rtt=auto",
            buildYandexMapsRouteUrl(latitude = 53.902284, longitude = 27.561831),
        )
    }

    @Test
    fun buildsAppleAndGoogleDrivingRoutes() {
        assertEquals(
            "https://maps.apple.com/?daddr=53.902284,27.561831&dirflg=d",
            buildAppleMapsRouteUrl(53.902284, 27.561831),
        )
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=53.902284,27.561831&travelmode=driving",
            buildGoogleMapsRouteUrl(53.902284, 27.561831),
        )
    }
}
