package com.coffeepeek.admin.ui.screen.shop

internal data class ShopRouteOption(val label: String, val url: String)

internal expect fun shopRouteOptions(latitude: Double, longitude: Double): List<ShopRouteOption>

internal fun buildYandexMapsRouteUrl(latitude: Double, longitude: Double): String =
    "https://yandex.ru/maps/?mode=routes&rtext=~$latitude,$longitude&rtt=auto"

internal fun buildAppleMapsRouteUrl(latitude: Double, longitude: Double): String =
    "https://maps.apple.com/?daddr=$latitude,$longitude&dirflg=d"

internal fun buildGoogleMapsRouteUrl(latitude: Double, longitude: Double): String =
    "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving"
