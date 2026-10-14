package com.coffeepeek.admin.ui.screen.shop

internal actual fun shopRouteOptions(latitude: Double, longitude: Double): List<ShopRouteOption> = listOf(
    ShopRouteOption("Google Карты", buildGoogleMapsRouteUrl(latitude, longitude)),
    ShopRouteOption("Яндекс Карты", buildYandexMapsRouteUrl(latitude, longitude)),
)
