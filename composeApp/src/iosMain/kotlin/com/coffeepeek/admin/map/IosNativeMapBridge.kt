@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.map

import platform.UIKit.UIView

/**
 * Objective-C-compatible seam between shared Compose map state and the required
 * native MapLibre implementation registered by the iOS application at startup.
 */
interface IosNativeMapCallbacks {
    fun onShopClick(shopId: String)
    fun onZoneClick(zoneId: String)
    fun onBoundsChanged(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        zoom: Float,
    )
}

interface IosNativeMapProvider {
    fun createMapView(): UIView

    fun updateMapView(
        mapView: UIView,
        stateJson: String,
        callbacks: IosNativeMapCallbacks,
    )

    fun moveCamera(
        mapView: UIView,
        latitude: Double,
        longitude: Double,
        zoom: Float,
        animated: Boolean,
    )
}

object IosNativeMapRegistry {
    var provider: IosNativeMapProvider? = null
}
